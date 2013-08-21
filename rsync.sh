rsync -avz --delete \
  --exclude "WEB-INF/appengine-generated" \
  target/realtime-server-0.3.0-SNAPSHOT \
  dev@192.168.1.15:/home/dev/dev/workspace/realtime/realtime-server/target
