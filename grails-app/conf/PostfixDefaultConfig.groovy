grails{
    plugin{
        postfix{
            defaultDomain = 'seaturtle'
            virtualMailboxPath = '/etc/postfix/vmailbox'
            userMailboxPath = '/var/mail/vhosts/'
            testing = false
            configureDovecotUsers = true
            imapDbName = 'dovecot_mail'
            imapDbUser = 'vs'
            imapDbPassword = 'vs'
        }
    }
}