FROM docker.bintray.io/jfrog/artifactory-oss:latest
COPY ./artifactory.config.xml /var/opt/jfrog/artifactory/etc/artifactory/artifactory.config.import.xml
COPY --chmod=777 ./security_descriptor.xml /var/opt/jfrog/artifactory/etc/artifactory/security.import.xml
