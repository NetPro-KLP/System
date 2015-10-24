#NetPro-KLP Back-end
##Requirement
```
clone this open source to '/home/system/' location
```
```
download jar file to libForVert.x(directory) as follows
- mkdir libForVert.x
- logback-classic-1.0.6.jar
- logback-core-1.0.6.jar
- mod-socket-io-1.0.2.jar
- mod-socket-io-1.0.3.jar
- mysql-connector-java-5.1.36-bin.jar
- slf4j-api-1.6.6.jar
```
```
download jar file to lib(directory) as follows
- mkdir lib
- vertx-core-2.0.0-final.jar
- vertx-platform-2.0.0-final.jar
```
```
1. download vert.x-2.0.0-final version
2. unzip to '/usr/local/vert.x/' location
3. edit '.bash_profile' as follows

export VERTX_HOME=/usr/local/vert.x/vert.x-2.0.0-final
export VERTX=$VERTX_HOME/bin
export PATH=$VERTX:$PATH
```
##FirewallServer

####expired data protocol
```
header 12 byte
- firewall ip	 	4byte			
- row number 		4byte			
- code 				4byte			
```
```
payload 31byte
- source_ip 		4byte
- source_port 		4byte
- destination_ip 	4byte
- destination_port 	4byte
- protocol 			4byte
- tcp/udp 			1byte
- warn 				4byte
- danger 			4byte
- packet_count 		4byte
- totalbytes 		4byte
```
####initialize ruleset protocol
```
header 4byte
- ruleset number	4byte
```
```
payload (N)byte
- data length		4byte
- data				(N)byte
```

##WebServer

####response to web front-end
```
200 - 클라이언트의 요청을 정상적으로 수행하였을때 사용.
	  응답 body에 요청과 관련된 내용 삽입.  
201 - 클라이언트가 어떤 리소스 생성을 요청하였고 해당 리소스가 성공적으로 생성되었을때 사용.
202 - 클라이언트의 요청이 비동기적으로 처리될 때 사용.
	  응답 body에 처리되기까지의 시간 등의 정보를 삽입.  
204 - 클라이언트의 요청을 정상적으로 수행하였을때 사용.
      200과 다르게 응답 바디가 없을때 사용.
      ex) DELETE와 같은 요청시에 사용 함.
```

```
400 - 클라이언트의 요청이 부적절할때 사용.
	  응답 body에 요청이 실패한 이유 삽입.
401 - 클라이언트가 인증되지 않은 상태에서 보호된 리소스를 요청했을때 사용.
	  예를들어 로그인 하지 않은 사용자가 로그인 했을때에만 요청 가능한 리소스를 요청했을 때의 응답.  
404 - 클라이언트가 요청한 리소스가 존재 하지 않을때 사용.
405 - 클라이언트가 요청한 리소스에서는 사용 불가능한 Method를 이용했을때 사용하는 응답.
	  ex) 읽기전용 리소스에 DELETE Method를 사용한 경우.
```
      
```   
301 - 클라이언트가 요청한 리소스에 대한 URI가 변경 되었을때 사용.
	  응답시 Location header에 변경된 URI를 삽입.
500 - 서버에 뭔가 문제가 있을때 사용.
```