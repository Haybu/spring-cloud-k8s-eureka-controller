# Spring Cloud Service Registry Controller

A Kubernetes controller implementation using Fabric8 Java-Client as a proxy
to register services' endpoints into a Eureka Server. 

![eureka controller](./images/k8s-eureka-controller.png)

A service needs to be labeled with `cloud.spring.io/enabled: "true"` to be picked 
and `cloud.spring.io/register: "true"` to enable it to register.

Run the manifest file as below to install the controller and 
a Eureka server

```bash
$ kubectl create -f ./deploy/manifest_kubernetes.yaml
```

Create a sample service and note that it register in Eureka

```bash
$ kubectl create -f ./sample/examle.yaml
```

To access the Eureka server check the exposed services IP:port in your cluster

```bash
$ kubectl get svc
```

If you are running a local cluster such as Minikube, you would need
to change the Eureka service in the manifest to be of "NodePort" type.
And note the service URL using ``` minikube service eureka-server --url```