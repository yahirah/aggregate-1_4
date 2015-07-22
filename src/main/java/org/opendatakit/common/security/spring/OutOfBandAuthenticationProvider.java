/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.common.security.spring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.common.security.SecurityUtils;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Repackaged WrappingOpenIDAuthenticationProvider that attaches the 
 * AUTH_OUT_OF_BAND grant to the recognized user.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class OutOfBandAuthenticationProvider implements AuthenticationProvider, InitializingBean {

  private static final Log logger = LogFactory.getLog(OutOfBandAuthenticationProvider.class);

  //~ Instance fields ================================================================================================

	UserDetailsService wrappingUserDetailsService;
	
	public UserDetailsService getWrappingUserDetailsService() {
		return wrappingUserDetailsService;
	}

	public void setWrappingUserDetailsService(
			UserDetailsService wrappingUserDetailsService) {
		this.wrappingUserDetailsService = wrappingUserDetailsService;
	}

	public void afterPropertiesSet() throws Exception {
		if ( wrappingUserDetailsService == null ) {
			throw new IllegalStateException("wrappingUserDetailsService must be defined");
		}
	}
	
   /* (non-Javadoc)
    * @see org.springframework.security.authentication.AuthenticationProvider#supports(java.lang.Class)
    */
   public boolean supports(Class<?> authentication) {
       return OutOfBandAuthenticationToken.class.isAssignableFrom(authentication);
   }

   /* (non-Javadoc)
    * @see org.springframework.security.authentication.AuthenticationProvider#authenticate(org.springframework.security.Authentication)
    */
   public Authentication authenticate(final Authentication authentication) throws AuthenticationException {

       if (!supports(authentication.getClass())) {
           return null;
       }

       if (authentication instanceof OutOfBandAuthenticationToken) {
         OutOfBandAuthenticationToken response = (OutOfBandAuthenticationToken) authentication;
           // Lookup user details
           UserDetails userDetails =
               new User(response.getName(), UUID.randomUUID().toString(), 
               true, true, true, true, new ArrayList<GrantedAuthority>() ); 
           return createSuccessfulAuthentication(userDetails, response);
       }

       return null;
   }

   /**
    * Handles the creation of the final <tt>Authentication</tt> object which will be returned by the provider.
    * <p>
    * The default implementation just creates a new OutOfBandAuthenticationToken from the original, but with the
    * UserDetails as the principal and including the authorities loaded by the UserDetailsService.
    *
    * @param userDetails the loaded UserDetails object
    * @param auth the token passed to the authenticate method, containing
    * @return the token which will represent the authenticated user.
    */
	protected Authentication createSuccessfulAuthentication(
			UserDetails rawUserDetails, OutOfBandAuthenticationToken auth) {
		String eMail = auth.getEmail();
		if ( eMail == null ) {
		   logger.warn("OutOfBand attributes did not include an e-mail address! ");
			throw new UsernameNotFoundException("email address not supplied in OutOfBand attributes");
		}
		eMail = OutOfBandAuthenticationProvider.normalizeMailtoAddress(eMail);
		String mailtoDomain = OutOfBandAuthenticationProvider.getMailtoDomain(eMail);
		
		UserDetails userDetails = rawUserDetails;
		
		Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
		
		authorities.addAll(userDetails.getAuthorities());
		// add the AUTH_OUT_OF_BAND granted authority,
		authorities.add(new SimpleGrantedAuthority(GrantedAuthorityName.AUTH_OUT_OF_BAND.toString()));

		// attempt to look user up in registered users table...
		String username = null;
		UserDetails partialDetails = null;
		boolean noRights = false;
		try {
			partialDetails = wrappingUserDetailsService.loadUserByUsername(eMail);
			// found the user in the table -- fold in authorizations and get uriUser.
			authorities.addAll(partialDetails.getAuthorities());
			// users are blacklisted by registering them and giving them no rights.
			noRights = partialDetails.getAuthorities().isEmpty();
			username = partialDetails.getUsername();
		} catch (Exception e) {
		  logger.warn("OutOfBand attribute e-mail: " + eMail + " did not match any known e-mail addresses! " + e.getMessage());
        throw new UsernameNotFoundException("account not recognized");
		}

		AggregateUser trueUser = new AggregateUser(username, 
													partialDetails.getPassword(),
													UUID.randomUUID().toString(), // junk...
													mailtoDomain,
													partialDetails.isEnabled(),
													partialDetails.isAccountNonExpired(),
													partialDetails.isCredentialsNonExpired(),
													partialDetails.isAccountNonLocked(),
													authorities);
		if ( noRights || !( trueUser.isEnabled() && trueUser.isAccountNonExpired() &&
				trueUser.isAccountNonLocked() ) ) {
         logger.warn("OutOfBand attribute e-mail: " + eMail + " account is blocked! ");
			throw new UsernameNotFoundException("account is blocked");
		}
		
        return new OutOfBandAuthenticationToken(trueUser, trueUser.getAuthorities(),
                                                 auth.getEmail());
    }

	public static final String getMailtoDomain( String uriUser ) {
		if ( uriUser == null ||
			 !uriUser.startsWith(SecurityUtils.MAILTO_COLON) ||
			 !uriUser.contains(SecurityUtils.AT_SIGN) )
			return null;
		return uriUser.substring(uriUser.indexOf(SecurityUtils.AT_SIGN)+1);
	}

	public static final String normalizeMailtoAddress(String emailAddress) {
		String mailtoUsername = emailAddress;
		if ( !emailAddress.startsWith(SecurityUtils.MAILTO_COLON) ) {
			if ( emailAddress.contains(SecurityUtils.AT_SIGN) ) {
				mailtoUsername = SecurityUtils.MAILTO_COLON + emailAddress;
			} else {
			   logger.warn("OutOfBand attribute e-mail: " + emailAddress + " does not specify a domain! ");
	         throw new IllegalStateException("e-mail address is incomplete - it does not specify a domain!");
			}
		}
		return mailtoUsername;
	}
}
