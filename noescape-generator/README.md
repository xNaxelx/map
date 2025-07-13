# How to build

```shell
podman build -t localhost:32000/noescape:latest ../noescape
```

Forward local port to remote registry
```shell
ssh -f -N -L 32000:localhost:32000 135.181.157.51
```

Push image to registry
```shell
podman push localhost:32000/noescape:latest
```
