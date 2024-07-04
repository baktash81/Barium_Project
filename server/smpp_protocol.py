import smpplib.client
import smpplib.gsm
import smpplib.consts
import time

from Crypto.Cipher import AES
from Crypto.Hash import SHA256
from base64 import b64encode, b64decode
from Crypto.Util.Padding import pad, unpad
from db import ClientDatabase  

class SMPPMessageHandler:
    def __init__(self):
        # Initialize the ClientDatabase instance
        self.db = ClientDatabase()
    
    def aes_encrypt(self, message, key):
        # Encrypt message using AES (CBC mode) with the given key
        iv = b'\x00' * AES.block_size
        cipher = AES.new(key.encode("utf-8"), AES.MODE_CBC, iv=iv)
        ciphertext = cipher.encrypt(pad(message.encode("utf-8"), AES.block_size))
        return b64encode(ciphertext).decode('utf-8')

    def aes_decrypt(self, ciphertext, key):
        # Decrypt message using AES (CBC mode) with the given key
        iv = b'\x00' * AES.block_size
        data = b64decode(ciphertext)
        cipher = AES.new(key.encode("utf-8"), AES.MODE_CBC, iv=iv)
        decrypted_data = cipher.decrypt(data)
        return unpad(decrypted_data, AES.block_size).decode('utf-8')

    def sha256_hash(self, message):
        # Generate SHA-256 hash of the given message
        hasher = SHA256.new()
        hasher.update(message.encode('utf-8'))
        return hasher.hexdigest()

    def send_sms(self, client, src_addr, dst_addr, message):
        # Send SMS using the SMPP client
        parts, encoding_flag, msg_type_flag = smpplib.gsm.make_parts(message)

        for part in parts:
            pdu = client.send_message(
                source_addr_ton=smpplib.consts.SMPP_TON_INTL,
                source_addr_npi=smpplib.consts.SMPP_NPI_ISDN,
                source_addr=src_addr,
                dest_addr_ton=smpplib.consts.SMPP_TON_INTL,
                dest_addr_npi=smpplib.consts.SMPP_NPI_ISDN,
                destination_addr=dst_addr,
                short_message=part,
                data_coding=encoding_flag,
                esm_class=msg_type_flag,
                registered_delivery=True,
            )
            print(f"Sent SMS with PDU: {pdu}")

    def handle_incoming_sms(self, pdu, client):
        # Handle incoming SMS PDU
        print(f"Received SMS PDU: {pdu}")
        source_address = pdu.source_addr.decode()
        print(f"From address: {source_address}")  

        if str(source_address) != "11111":
            print(f"To address: {pdu.destination_addr.decode()}")
            encrypted_message = pdu.short_message.decode("utf-8")
            print(f"Encrypted content: {encrypted_message}")
            encrypted_message = str(encrypted_message).split("\x02\x02")[1]
            encrypted_message_bytes = encrypted_message.encode("iso-8859-1")
            print(f'Processed encrypted message: {encrypted_message}')
            key = "d8g3b1h7j9k0l6m4n2p5q9r8s7t3u1v0"
            decrypted_message = self.aes_decrypt(encrypted_message_bytes, key)
            print(f"Decrypted content: {decrypted_message}")

            self.db.add_or_update_client(self.db.connection, decrypted_message, source_address)

            hashed_message = self.sha256_hash(decrypted_message)
            hashed_message = hashed_message + " | SHAKBAK"
            
            encrypted_feedback = self.aes_encrypt(hashed_message, key)
            self.send_sms(client, pdu.destination_addr.decode(), source_address, f'$$ {encrypted_feedback} $$')
    
    def receive_sms(self, client):
        # Listen for incoming SMS messages
        while True:
            try:
                client.set_message_received_handler(lambda pdu: self.handle_incoming_sms(pdu, client))
                print("Awaiting incoming SMS...")
                client.listen()
            except KeyboardInterrupt:
                print("Listener stopped by user.")
                break
            except Exception as e:
                print(f"An error occurred: {e}")
                print("Attempting to reconnect...")
                time.sleep(5)
                client.connect()
                client.bind_transceiver(system_id=username, password=password)

    def send_and_receive_sms(self, host, port, username, password, src_addr):
        # Connect to SMPP server and start sending/receiving SMS
        with smpplib.client.Client(host, port) as client:
            client.connect()
            client.bind_transceiver(system_id=username, password=password)
            self.receive_sms(client)
            client.unbind()
            client.disconnect()

# Initialize and start the SMPP message handler
smpp_handler = SMPPMessageHandler()
host = '172.20.10.6'
port = 9500
username = "smppuser"
password = "YeNVqx6d"
src_addr = '9377426044'
smpp_handler.send_and_receive_sms(host, port, username, password, src_addr)
