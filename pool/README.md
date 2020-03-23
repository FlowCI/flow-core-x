## Enable Docker HTTP API

To configure the Docker daemon using a JSON file, 
create a file at `/etc/docker/daemon.json` on Linux systems, 
add the flowing into JSON file.

```json
{
  "tlscert": "/var/docker/server.pem",
  "tlskey": "/var/docker/serverkey.pem",
  "hosts": ["unix:///var/run/docker.sock", "tcp://0.0.0.0:2376"]
}
```