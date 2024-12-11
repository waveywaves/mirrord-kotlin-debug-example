# Debugging Java Applications with mirrord

<div align="center">
  <a href="https://mirrord.dev">
    <img src="images/mirrord.svg" width="150" alt="mirrord Logo"/>
  </a>
  <a href="https://kotlinlang.org">
    <img src="images/kotlin.svg" width="150" alt="Java Logo"/>
  </a>
</div>


A sample Kotlin-based guestbook application demonstrating how to debug Kotlin applications using mirrord. This application features a web interface for posting and viewing guestbook entries, with Redis as the backend storage.

## Tech Stack

- Kotlin 1.6.10
- Java 17
- Redis
- Kubernetes

## Features

- Web-based guestbook interface
- Real-time entry updates (10-second refresh)
- Redis-backed storage
- Kubernetes-ready deployment

## Prerequisites

- Java 17 or higher
- Maven
- Kubernetes cluster (for k8s deployment)
- Redis instance

## Local Development

1. Deploy the application to your Kubernetes cluster:

```bash
minikube start
kubectl apply -f kube
```

2. Run the application locally with mirrord:

```bash
mirrord exec -t deployment/kotlin-guestbook -- mvn compile exec:java
```

## How it Works

mirrord allows you to run your local Kotlin application while stealing traffic from your Kubernetes cluster. This means:

- Network traffic intended for your pod is intercepted and sent to your local instance
- Your local application can access cluster resources (MongoDB, MinIO) as if it were running in the cluster
- You can debug your application locally while it processes real cluster traffic

## Configuration

The mirrord configuration is stored in `mirrord.json` and specifies:
- Network traffic stealing
- File system access
- Environment variable copying
- Target deployment details

## License

This project is licensed under the MIT License - see the LICENSE file for details.