SIPSpeaker
==========

SIP Speaker Assignment for the IK2213 course. It is composed of a basic SIP server that can read message from a WAV file and a web server to change the text message that will be read by the SIP server.


How to compile
-----------------------
The program is already compiled and ready to use. But you can still do some modification and compile it again very easily with eclipse for example. You just need to be sure that the path of the external lib are in the classpath.


How to configure
-----------------------
All the parameters of interest can be change in the sipspeaker.cfg file. The program will use these values during the start. You can still make another conf file and use it with the -c option.
You can change the default message of the server, the SIP user, the SIP port, the SIP interface, the HTTP port, the HTTP interface and the location of the wav message with the conf file.


How to run
-----------------------
Juste write " java -jar SIP.jar [-c config_file_name] [-user sip_uri] [-http http_bind_address]".
* -c config_file_name specifies the configuration file. The file name can include a relative or absolute path.
* -user sip_uri indicates what address the SIP server should listen to, for example robot@127.0.0.1:5064.
* -http http_bind_address specifies the address and port number where the web server listens for incoming connections. Example: "127.0.0.1:8080", or only port number "8080", or only address/name: "myserver.mydomain.se".
The jar file must be launch in the same folder as the lib folder and the Web folder in order to have access to these resources otherwise it won't work.
You might want to run the program as a root user depending on the port you want to use.


How to use
-----------------------
This program has been tested on Linphone 3.6, we do not guarantee its use with another software.
On linphone just write user@IP:port to start the call. To access to the web page to edit the message just write IP:port in a web browser. This is obvious from here.