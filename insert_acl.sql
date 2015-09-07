INSERT INTO acl_class (class) VALUES ('org.opendatakit.aggregate.form.Form');

INSERT INTO acl_sid (principal, sid) VALUES ('true', 'anna');

INSERT INTO acl_object_identity (object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting) VALUES (1, 1, NULL, 1, 'false');

INSERT INTO acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure) VALUES (1, 1, 1, 1, 'true', 'true', 'true');