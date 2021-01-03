# Airsonic on Kubernetes

## Prerequisites

- A Kubernets Cluster (v1.18 or later)
- `kubectl` is configured

## Deploy

1. Copy the file [airsonic-kubernetes.yaml](airsonic-kubernetes.yaml) locally.

1. Edit `airsonic-kubernetes.yaml` to configure access to your media share.

  > Note: See this document for additional volume types: https://kubernetes.io/docs/concepts/storage/volumes/

1. Deploy

  ```shell
  kubectl create namespace airsonic
  kubectl apply -f airsonic-kubernetes.yaml --namespace airsonic
  ```

## Access

1. Determine the external IP address given to the Airsonic service
  ```shell
  kubectl get services --namespace airsonic
  ```

  Output should look similar to this

  ```
  NAME       TYPE           CLUSTER-IP       EXTERNAL-IP       PORT(S)        AGE
  airsonic   LoadBalancer   10.XXX.XXX.XXX   73.XXX.XXX.XXX    80:30413/TCP   100m
  ```

1. Open that IP address in a web browser.

  ```shell
  open http://73.XXX.XXX.XXX
  ```

1. Login

  > Note: the prompt will tell you then default username and password for initial installs

1. [Configure your Airsonic server](https://airsonic.github.io/docs/first-start/)

## TO DO

- Expose with an ingress proxy with TLS

- Supply configuration using a `ConfigMap`

- Deploy with an external database
