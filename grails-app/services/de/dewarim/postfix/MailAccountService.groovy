/*
 * Copyright (c) 2012 Ingo Wiarda
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and 
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE
 */
package de.dewarim.postfix

import org.apache.commons.codec.digest.DigestUtils

class MailAccountService {

    def grailsApplication
    
    static PostfixConfigActor configActor
    
    static {
        configActor = new PostfixConfigActor()
        configActor.start()
    }

    /**
     * List the mail accounts for a specific or all domains.
     * @param domain optional parameter: the mail domain, if null returns all mail accounts configured in vmailbox
     * @return a PostfixConfigResult which contains the list of users (and/or error messages)
     */
    def listAccounts(String domain) {
        def configCommand = new ConfigCommand(type: CommandType.LIST_USERS, 
                config: grailsApplication.mergedConfig?.grails?.plugin?.postfix,
                mailDomain:domain
        )
        return configActor.sendAndWait(configCommand)
    }

    /**
     * Add a user to the vmailbox and update Postfix.
     * @param name the username (the part before the @ in user@domain)
     * @param domain the name of the mail domain (the part after the @ in user@domain)
     * @return a PostfixConfigResult to determine if the command succeeded.
     */
    def addAccount(String name, String domain, String passwordHash){
        def configCommand = new ConfigCommand(type: CommandType.ADD_USER, 
                config: grailsApplication.mergedConfig?.grails?.plugin?.postfix,
                username: name,
                mailDomain: domain,
                passwordHash:passwordHash
        )
        configActor.sendAndWait(configCommand)
    }

    /**
     * Remove a user from the vmailbox and update Postfix.
     * @param name the username (the part before the @ in user@domain)
     * @param domain the name of the mail domain (the part after the @ in user@domain)
     * @return a PostfixConfigResult to determine if the command succeeded.
     */
    def deleteAccount(String name, String domain){
        def configCommand = new ConfigCommand(type: CommandType.DELETE_USER,
                config: grailsApplication.mergedConfig?.grails?.plugin?.postfix,
                username: name,
                mailDomain: domain
        )
        configActor.sendAndWait(configCommand)
    }

    /**
     * Deactivate a user in the vmailbox and update Postfix. This means the user is commented out
     * in the vmailbox file, which may help to keep track of who has had a mail box earlier. 
     * @param name the username (the part before the @ in user@domain)
     * @param domain the name of the mail domain (the part after the @ in user@domain)
     * @return a PostfixConfigResult to determine if the command succeeded.
     */
    def deactivateAccount(String name, String domain){
        def configCommand = new ConfigCommand(type: CommandType.DEACTIVATE_USER, 
                config: grailsApplication.mergedConfig?.grails?.plugin?.postfix,
                username: name,
                mailDomain: domain
        )
        configActor.sendAndWait(configCommand)
    }
    
    def updateLocalPassword(String name, String domain, String password){
        Auth auth = Auth.dovecot_mail.find("from Auth a where a.username=:name and a.domain=:domain",
                [name:name, domain:domain])
        def pwd = "{SHA256.HEX}${DigestUtils.sha256Hex(password)}"
        if(auth){
            auth.password = pwd
        }
        else{
            auth = new Auth(username: name, domain: domain, localEntry: true, email: "${name}@${domain}", password: pwd)
            auth.save()
        }
    }
}
