export DEV_SERVER=$HOME/.m2/repository/com/google/appengine/appengine-java-sdk/1.8.1.1/appengine-java-sdk/appengine-java-sdk-1.8.1.1/bin/dev_appserver.sh
chmod 770 $DEV_SERVER
$DEV_SERVER \
  --jvm_flag=-Dfile.encoding=UTF-8 \
  --disable_update_check --address=0.0.0.0 \
  target/realtime-server-appengine-0.3.0-SNAPSHOT
