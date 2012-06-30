dataSource {
    pooled = true
//    driverClassName = "org.postgresql.Driver"
//    url = "jdbc:postgresql://127.0.0.1:5432/dovecot_mail"
//    dialect = "org.hibernate.dialect.PostgreSQLDialect"
//    username = "vs"
//    password = "vs"
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
}
// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "none" // one of 'create', 'create-drop', 'update', 'validate', ''
        }

        dataSource_dovecot_mail{
            pooled = true
            driverClassName = "org.postgresql.Driver"
            url = "jdbc:postgresql://127.0.0.1:5432/dovecot_mail"
            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            username = "vs"
            password = "vs"
            properties{
                initialSize = 10
                maxActive = 500
                minEvictableIdleTimeMillis=300000
                timeBetweenEvictionRunsMillis=300000
                numTestsPerEvictionRun=5
                testOnBorrow=true
                testWhileIdle=true
                testOnReturn=true
                validationQuery="SELECT 1"
            }
        }
        
    }
    test {
        dataSource {
            dbCreate = "none"
        }
    }
    production {
        dataSource {
            dbCreate = "none"
            pooled = true
            properties {
               maxActive = -1
               minEvictableIdleTimeMillis=1800000
               timeBetweenEvictionRunsMillis=1800000
               numTestsPerEvictionRun=3
               testOnBorrow=true
               testWhileIdle=true
               testOnReturn=true
               validationQuery="SELECT 1"
            }
        }

        dataSource_dovecot_mail{
            pooled = true
            dbCreate = 'none'
            driverClassName = "org.postgresql.Driver"
            url = "jdbc:postgresql://127.0.0.1:5432/dovecot_mail"
            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            username = "vs"
            password = "vs"
            properties{
                initialSize = 10
                maxActive = 500
                minEvictableIdleTimeMillis=300000
                timeBetweenEvictionRunsMillis=300000
                numTestsPerEvictionRun=5
                testOnBorrow=true
                testWhileIdle=true
                testOnReturn=true
                validationQuery="SELECT 1"
            }
        }
    }
}
