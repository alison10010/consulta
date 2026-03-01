FROM quay.io/wildfly/wildfly:36.0.1.Final-jdk17

ARG WAR_FILE=target/security.war

# Copia o WAR para o WildFly
COPY ${WAR_FILE} /opt/jboss/wildfly/standalone/deployments/
RUN touch /opt/jboss/wildfly/standalone/deployments/security.war.dodeploy

# Expor somente a porta HTTP
EXPOSE 8080 8787

# Variável para configuração do Oracle NLS
ENV NLS_LANG=AMERICAN_AMERICA.AL32UTF8

# Configurações regionais da JVM
ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787 -Duser.country=BR -Duser.language=pt"

# Comando para iniciar o WildFly com binding para todas as interfaces
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]


# PRODUCAO ABAIXO

#FROM quay.io/wildfly/wildfly:36.0.1.Final-jdk17

# Adiciona usuário administrativo
#RUN /opt/jboss/wildfly/bin/add-user.sh -a -u acreprev -p Admin#2005@ --silent

# Copia o WAR para a pasta de deploy
#COPY security.war /opt/jboss/wildfly/standalone/deployments/

# Cria o marker de deploy
#RUN touch /opt/jboss/wildfly/standalone/deployments/security.war.dodeploy

# Expor a porta padrão
#EXPOSE 8080

# Comando de inicialização
#CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]

