import sqlite3

class ClientDatabase:
    def __init__(self):
        # Initialize and set up the database
        self.connection = self.initialize_database()

    def initialize_database(self):
        # Connect to the SQLite database (creates a new one if it doesn't exist)
        connection = sqlite3.connect("clients.db")
        cursor = connection.cursor()

        # Create the clients table if it doesn't exist
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS clients (
            address TEXT PRIMARY KEY,
            client_location TEXT,
            client_value TEXT,
            cell_information TEXT           
        )
        """)

        connection.commit()
        return connection

    def add_or_update_client(self, connection, message, address):
        cursor = connection.cursor()

        # Check if the address already exists in the database
        cursor.execute("SELECT * FROM clients WHERE address=?", (address,))
        record = cursor.fetchone()

        # Extract client_location, client_value, and cell_information from the message
        client_location = message.split("LOC")[1].split("\n")[0]
        client_value = message.split("VAL")[1].split("\n")[0]
        cell_information = message.split("CINFO")[1].split("\n")[0]

        # Display extracted information
        print("Extracted Location:", client_location)
        print("Extracted Value:", client_value)
        print("Extracted Cell Info:", cell_information)

        if record:
            # Update existing record
            cursor.execute("""
            UPDATE clients SET client_location=?, client_value=?, cell_information=? WHERE address=?
            """, (client_location, client_value, cell_information, address))
        else:
            # Insert new record
            cursor.execute("""
            INSERT INTO clients (address, client_location, client_value, cell_information) VALUES (?, ?, ?, ?)
            """, (address, client_location, client_value, cell_information))

        connection.commit()
