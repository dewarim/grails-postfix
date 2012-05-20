#Grails Postfix plugin

A simple plugin to manage users of virtual mailbox domains.

The MailAccountService allows you to list, add, remove and deactivate user accounts in the Postfix vmailbox file. This
plugin assumes that the account running the code has write permissions to the vmailbox and the virtual.db file.

##Configuration options:

  grails{
      plugin{
          postfix{
            defaultDomain = 'seaturtle'
            virtualMailboxPath = '/etc/postfix/vmailbox'
            testing = false
          }
      }
  }

Code Maturity: alpha! Needs testing, do not use on production systems!

 Version: 0.1.0
 Author: Ingo Wiarda
 Mail: ingo_wiarda@dewarim.de
 Website and Repository: currently https://github.com/dewarim/grails-postfix
 License: Apache License 2.0 / http://www.apache.org/licenses/LICENSE-2.0.html 
