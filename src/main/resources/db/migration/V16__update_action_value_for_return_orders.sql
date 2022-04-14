update public.audit_log
    set action = 'RETURN_ORDER_CREATED'
    where action = 'RETURN_DELIVERY_NOTE_PRINTED';
