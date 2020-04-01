using minikue

$ kubectl run nginx --image=nginx --restart=Always -n springcloud -l config.spring.io/enabled=true --expose --port 60555