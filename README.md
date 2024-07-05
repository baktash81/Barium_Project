# Barium_Project

The expansion of mobile networks, especially 4G and 5G, has made them the largest accessible networks for the internet. Security of applications and secure communication is one of the main challenges in this field. In this project, we assume that an application communicates with a server via SMS, and the security and implementation aspects of this communication will be considered. This communication should be documented and based on the SMPP protocol.

For better understanding, consider having two phones: one for the client and one acting as the server. On the server phone, an Android application with backend functionality is installed. The client sends commands to the server via SMS. The client must continuously send information regarding signal strength, the serving cell technology, and the location of these readings to the server whenever the signal strength falls below a certain threshold. Additionally, when the client sends a request to the server, the server should process the request and send a response back to the client in a separate SMS.

This setup ensures effective monitoring and secure communication between the client and server, leveraging the widespread availability and reliability of mobile networks.


* This project is implemented for the Introduction To Mobile Networks course in IUST during 2024 spring.
