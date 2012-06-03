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

import groovyx.gpars.actor.DefaultActor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.dewarim.postfix.Auth
/**
 * An Actor class to encapsulate updates to postfix config files. This actor makes sure that
 * no two threads write or update postfix configuration files at the same time.
 */
class PostfixConfigActor extends DefaultActor {

    Logger log = LoggerFactory.getLogger(this.class)

    List<String> mailDomains = []

    void onDeliveryError(msg) {
        log.warn("Could not deliver msg: $msg")
    }

    protected void act() {
        loop {
            react { command ->
                try {
                    PostfixConfigResult result = new PostfixConfigResult()
                    try {
                        switch (command.type) {
                            case CommandType.ADD_USER: addUser(command); break
                            case CommandType.DEACTIVATE_USER: deactivateUser(command); break
                            case CommandType.DELETE_USER: deleteUser(command); break
                            case CommandType.LIST_USERS: result.users = listUsers(command); break
                        }
                    }
                    catch (Exception e) {
                        log.debug("command failed: ", e)
                        result.failed = true
                        result.errors.add(e.message)
                    }
//                    log.debug("reply & finish")
                    reply result
                }
                catch (Exception e) {
                    log.debug("Failed to act on command:", e)
                }
            }
        }
    }

    def listUsers(command) {
        def lines = loadVirtualMailConfig(command).findAll {line ->
            if (command.mailDomain) {
                line =~ /@${command.mailDomain}\s+/
            }
            else {
                true
            }
        }
        def users = lines.collect {String line ->
            line.split('\\s+')[0]
        }
        return users
    }

    def loadVirtualMailConfig(command) {
        def virtualPath = command.config?.virtualMailboxPath
        if (!virtualPath) {
            throw new RuntimeException('Cannot find path to postfix virtual file in config.')
        }
        def virtual = new File(virtualPath)
        if (!virtual.exists()) {
            throw new RuntimeException("Virtual config file ${virtualPath} does not exist.")
        }
        def lines = virtual.readLines().findAll { line ->
            // an entry should look like this: ingo@seaturtle seaturtle/ingo/
            // skip lines starting with # as those are comments:
            line =~ /^[^#]+?@[^\s]+\s+\w+/
        }
        return lines
    }

    void addUser(ConfigCommand command) {
        List lines = loadVirtualMailConfig(command)
        def userEntry = generateUserEntry(command)
        if (lines.contains(userEntry)) {
            // nop
            log.debug("mail user already exists, will just update passwordHash")
            Auth.withTransaction{
                def auth = Auth.findByEmail("${command.username}@${command.mailDomain}")
                if (auth){
                    auth.pwd = command.passwordHash
                }
                else{
                    log.debug("Could not find entry in auth table.")
                }
            }
        }
        else {
            if (lines.contains("#$userEntry")) {
                log.debug("reactivate user $userEntry")
                lines.remove("#$userEntry")
            }
            lines.add(userEntry)
            writeConfig(command, lines)
            Auth.withTransaction {
                def auth = new Auth(email: "${command.username}@${command.mailDomain}",
                        domain: command.mailDomain,
                        username: command.username,
                        pwd: command.passwordHash,
                )
                auth.save()
            }
        }
    }

    void deleteUser(command) {
        List lines = loadVirtualMailConfig(command)
        def userEntry = generateUserEntry(command)
        if (lines.contains(userEntry)) {
            lines.remove(userEntry)
            writeConfig(command, lines)
            Auth.withTransaction {
                def auth = Auth.findByEmail("${command.username}@${command.mailDomain}")
                if (auth) {
                    auth.delete()
                }
            }
        }
    }

    void deactivateUser(command) {
        List lines = loadVirtualMailConfig(command)
        def userEntry = generateUserEntry(command)
        if (lines.contains(userEntry)) {
            lines.remove(userEntry)
            lines.add("#$userEntry")
            writeConfig(command, lines)
            Auth.withTransaction {
                def auth = Auth.findByEmail("${command.username}@${command.mailDomain}")
                if (auth) {
                    auth.active = false
                    auth.pwd = '---inactive---'
                }
            }
        }
    }

    String generateUserEntry(ConfigCommand command) {
        return "${command.username}@${command.mailDomain}\t${command.mailDomain}/${command.username}/"
    }

    void writeConfig(command, lines) {
        def commandFile = new File(command.config.virtualMailboxPath)
        commandFile.withWriter {writer ->
            lines.each {
//                log.debug("$it")
                writer.write(it + "\n")
            }
        }

        def ant = new AntBuilder()
        def cmdLine = """ ${command.config.virtualMailboxPath} """
        log.debug("executable: postmap, cmdLineParams: $cmdLine")
        if (command.config.testing) {
            // do not run postmap in test environment.
            return
        }
        ant.exec(outputproperty: "cmdOut",
                errorproperty: "cmdErr",
                resultproperty: "cmdExit",
                failonerror: "true",
                executable: 'postmap') {
            arg(line: cmdLine)
        }

        log.debug("err: ${ant.project.properties.cmdErr}")
        log.debug("out: ${ant.project.properties.cmdOut}")
    }

}
