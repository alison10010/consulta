FROM quay.io/wildfly/wildfly:36.0.1.Final-jdk17
ENV TZ=America/Rio_Branco

# NÃO usar ROOT.war
COPY consulta.war /opt/jboss/wildfly/standalone/deployments/consulta.war

EXPOSE 8080
ENV JAVA_OPTS="-Duser.country=BR -Duser.language=pt"
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
