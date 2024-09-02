workspace {

    model {

        user = person "Investidor" {
            description "Usuário que deseja identificar empresas de valor para investir."
        }

        softwareSystem = softwareSystem "GrahamSelect" {
        
            description "Aplicativo para calcular as 10 empresas mais baratas usando a regra de Benjamin Graham."
            
            kafka = container "Kafka" {
                description "Sistema de mensageria usado para a ingestão e processamento de dados"
                technology "Apache Kafka"
                tags "kafka"
            }
            
            database = container "Database" {
                description "Armazena os dados financeiros e resultados do cálculo"
                technology "MySql"
                tags "database"
            }       

            apiApplication = container "API (Spring Boot)" {
                description "API RESTful para fornecer dados das empresas mais baratas"
                technology "Java, Spring Boot"
                apiController = component "API Controller" {
                    description "Exposição dos endpoints para acessar os dados das empresas"
                    technology "Spring MVC"
                }
                geradorEventosService = component "Event Generator Service" {
                    description "Serviço geração dos eventos com dados das empresas"
                    technology "Java Service"
                }
                producerService = component "Producer Service" {
                    description "Publica eventos com dados das empresas no Kafka"
                    technology "Java Service"
                }
                companyFinancialDataRepository = component "Repositório de dados financeiros empresas" {
                    description "Serviço de repositório de empresas"
                    technology "JPA"
                    companyFinancialDataRepository -> database "Consulta tabela de dados de empresas"
                }
                rankCompaniesService = component "Rank Companies Service" {
                    description "Serviço de cáculo de ranking das empresas"
                    rankCompaniesService -> companyFinancialDataRepository "Consulta tabela de dados de empresas"
                }
                apiController -> geradorEventosService 
                apiController -> rankCompaniesService
                geradorEventosService -> producerService
                producerService -> kafka "Publica evento de dados de empresas"
            }
            
            webApplication = container "Frontend (Flutter)" {
                description "Interface gráfica para interação do usuário"
                technology "Flutter"
                webApplication -> apiController "Chama API de consulta de empresas mais baratas"
                webApplication -> apiController "Chama API de Upload de arquivo de dados"
            }
            
            dataProcessor = container "Financial Data Processor" {
                description "Processamento de dados financeiros"
                technology "Java, Spring Boot"
                companyFinancialDataRepository1 = component "Repositório de dados financeiros empresas" {
                    description "Serviço de repositório de empresas"
                    technology "JPA"
                    companyFinancialDataRepository1 -> database "Consulta tabela de dados de empresas"
                }                
                companyRepository = component "Repositório de empresas" {
                    description "Serviço de repositório de empresas"
                    technology "JPA"
                    companyRepository -> database "Consulta tabela de empresas"
                }                
                calculationService = component "Calculation Service" {
                    description "Realiza o cálculo de valor intrínseco das empresas"
                    technology "Java Service"
                    calculationService -> companyRepository "Gravar novas empresas"
                    calculationService -> companyFinancialDataRepository1 "Grava novos dados financeiros"
                }                
                consumer = component "New financial data Consumer" {
                    description "Consome os eventos de novos dados financeiros"
                    technology "Kafka Listener"
                    kafka -> consumer
                    consumer -> calculationService
                }                
            }
    
        }

        user -> softwareSystem "Consulta e analisa as empresas mais baratas"



        user -> webApplication "Usa"
        dataProcessor -> database "Armazena dados processados"

        calculationService -> database "Lê e grava dados"
        
        dev = deploymentEnvironment "Dev" {
            deploymentNode "Kubernetes Cluster" {
                containerInstance apiApplication
                containerInstance dataProcessor
                containerInstance webApplication
                containerInstance database
                containerInstance kafka
            }
        }
    }

    views {
        systemContext softwareSystem {
            include *
        }

        container softwareSystem {
            include *
        }

        component apiApplication {
            include *
            autolayout lr
        }
        
        component dataProcessor {
            include *
            autolayout lr
        }
        
        deployment * dev {
            include *
        }

        styles {
            element database {
                shape Cylinder
            }
            element kafka {
                shape Pipe
            }
        }


        theme default


    }


}
