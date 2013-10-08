mvn appengine:endpoints_get_discovery_doc appengine:endpoints_get_client_lib
cd target/generated-sources/appengine-endpoints/WEB-INF/
# unzip -q `ls | grep zip`
rm -rf device
unzip -q device-v0.0.1-java.zip
cd device
unzip -q `ls | grep sources.jar` -d maven
cd maven
sed -i.bak "s/com.google.apis/com.goodow.apis/;s/google-api-services/goodow-api-services/;s/v0.0.1-[^<]*/0.0.1-SNAPSHOT/" pom.xml
mvn clean deploy -Psonatype-oss-release

cd ../..
rm -rf account
unzip -q account-v0.0.1-java.zip
cd account
unzip -q `ls | grep sources.jar` -d maven
cd maven
sed -i.bak "s/com.google.apis/com.goodow.apis/;s/google-api-services/goodow-api-services/;s/v0.0.1-[^<]*/0.0.1-SNAPSHOT/" pom.xml
mvn clean deploy -Psonatype-oss-release

cd ../..
rm -rf presence
unzip -q presence-v0.0.1-java.zip
cd presence
unzip -q `ls | grep sources.jar` -d maven
cd maven
sed -i.bak "s/com.google.apis/com.goodow.apis/;s/google-api-services/goodow-api-services/;s/v0.0.1-[^<]*/0.0.1-SNAPSHOT/" pom.xml
mvn clean deploy -Psonatype-oss-release
