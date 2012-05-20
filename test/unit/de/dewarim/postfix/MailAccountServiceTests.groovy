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



import grails.test.mixin.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(MailAccountService)
class MailAccountServiceTests {

    def mailAccountService
    
    
    void testVirtualMailbox() {
        def virtualMailBox = """ingo@seaturtle seaturtle/ingo/
foo@seaturtle seaturtle/foo/
bar@seahorse seahorse/bar/""" 
        
        def tempBox = File.createTempFile("virtualMailBox_", ".txt")
        tempBox.write(virtualMailBox)
        
        mailAccountService = new MailAccountService()
        def mockConfig = new ConfigObject()
        mockConfig.grails.plugin.postfix.virtualMailboxPath = tempBox.absolutePath
        mockConfig.grails.plugin.postfix.defaultDomain = 'seaturtle'
        mockConfig.grails.plugin.postfix.testing = true
        mailAccountService.grailsApplication = new Expando(mergedConfig: mockConfig)
        
        // test if we can read the virtualMailBox at all:
        def result = (PostfixConfigResult) mailAccountService.listAccounts(null)
      
        assert ! result.failed : 'postfix failed.'
        def users = result.users  
        assert users.contains('foo@seaturtle')
        assert users.size() == 3
        
        // just find users of a specific domain:
        users = mailAccountService.listAccounts('seahorse').users
        assert users.size() == 1
        assert users.contains('bar@seahorse')
        
        // add a user:
        result = (PostfixConfigResult) mailAccountService.addAccount('postmistress', 'seahorse')
        assert ! result.failed
        users = mailAccountService.listAccounts('seahorse').users
        assert users.size() == 2
        assert users.contains('postmistress@seahorse')

        // deactivate user:
        result = (PostfixConfigResult) mailAccountService.deactivateAccount('postmistress', 'seahorse')        
        assert ! result.failed
        users = mailAccountService.listAccounts('seahorse').users
        assert users.size() == 1
        assert ! users.contains('postmistress@seahorse')
        assert tempBox.text.contains('#postmistress@seahorse')
        result = (PostfixConfigResult) mailAccountService.addAccount('postmistress', 'seahorse')
        assert ! result.failed
        
        // delete a user:
        result = (PostfixConfigResult) mailAccountService.deleteAccount('postmistress', 'seahorse')
        assert ! result.failed
        users = mailAccountService.listAccounts('seahorse').users
        assert users.size() == 1
        assert ! users.contains('postmistress@seahorse')
        
    }
}
