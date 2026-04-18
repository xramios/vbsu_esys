#!/usr/bin/env python3
"""
Database connection and adapter module.

Handles MySQL and Derby database connections, parameter formatting,
and database-specific operations with abstraction layer.
"""

from typing import Optional, List, Any, Union
from datetime import datetime
import mysql.connector
from mysql.connector import Error as MySQLError
import jaydebeapi
import jpype

from seeder.config.settings import DATABASE_CONFIG, DERBY_CONFIG, DERBY_JAR_PATH, DERBY_SHARED_JAR_PATH


class DatabaseAdapter:
    """Abstracts database-specific syntax and operations."""

    def __init__(self, db_type: str) -> None:
        """Initialize adapter with database type.

        Args:
            db_type: Database type ('mysql' or 'derby')
        """
        self.db_type = db_type.lower()

    def get_table_prefix(self) -> str:
        """Get table prefix based on database type.

        Returns:
            Table prefix string ('APP.' for Derby, '' for MySQL)
        """
        return "APP." if self.db_type == "derby" else ""

    def get_param_placeholder(self) -> str:
        """Get parameter placeholder based on database type.

        Returns:
            Parameter placeholder ('?' for Derby, '%s' for MySQL)
        """
        return "?" if self.db_type == "derby" else "%s"

    def format_datetime(self, dt: datetime) -> Union[str, datetime]:
        """Format datetime for database compatibility.

        Args:
            dt: Datetime object to format

        Returns:
            Formatted datetime (string for Derby, datetime for MySQL)
        """
        return dt.strftime("%Y-%m-%d") if self.db_type == "derby" else dt

    def format_timestamp(self, ts: datetime) -> Union[str, datetime]:
        """Format timestamp for database compatibility.

        Args:
            ts: Datetime object to format

        Returns:
            Formatted timestamp (string for Derby, datetime for MySQL)
        """
        return ts.strftime("%Y-%m-%d %H:%M:%S") if self.db_type == "derby" else ts

    def get_last_insert_id(self, cursor: Any, table_name: str) -> int:
        """Get the last inserted ID based on database type.

        Args:
            cursor: Database cursor object
            table_name: Name of the table to query

        Returns:
            The last inserted ID
        """
        if self.db_type == "mysql":
            return cursor.lastrowid

        # Derby: Use SELECT MAX(id)
        try:
            prefix = self.get_table_prefix()
            cursor.execute(f"SELECT MAX(id) FROM {prefix}{table_name}")
            result = cursor.fetchone()
            if result and result[0] is not None:
                return result[0]
            return 1
        except Exception:
            return 1

    def build_insert_query(self, table_name: str, columns: List[str]) -> str:
        """Build INSERT query with appropriate parameter placeholders.

        Args:
            table_name: Target table name
            columns: List of column names

        Returns:
            Formatted INSERT query string
        """
        prefix = self.get_table_prefix()
        placeholder = self.get_param_placeholder()
        column_list = ", ".join(columns)
        param_list = ", ".join([placeholder] * len(columns))
        return f"INSERT INTO {prefix}{table_name} ({column_list}) VALUES ({param_list})"


class DatabaseManager:
    """Manages database connections for MySQL and Derby."""

    def __init__(
        self,
        db_type: str = "mysql",
        host: Optional[str] = None,
        port: Optional[int] = None,
        database: Optional[str] = None,
        user: Optional[str] = None,
        password: Optional[str] = None,
    ) -> None:
        """Initialize database manager with connection parameters.

        Args:
            db_type: Database type ('mysql' or 'derby')
            host: Database host address
            database: Database name
            user: Database username
            password: Database password
        """
        self.db_type = db_type.lower()
        self.adapter = DatabaseAdapter(self.db_type)

        if self.db_type == "derby":
            self.host = host or DERBY_CONFIG["host"]
            self.port = port or DERBY_CONFIG.get("port", 1527)
            self.database = database or DERBY_CONFIG["database"]
            self.user = user or DERBY_CONFIG["user"]
            self.password = password or DERBY_CONFIG["password"]
        else:
            self.host = host or DATABASE_CONFIG["host"]
            self.port = port
            self.database = database or DATABASE_CONFIG["database"]
            self.user = user or DATABASE_CONFIG["user"]
            self.password = password or DATABASE_CONFIG["password"]

        self.connection: Optional[Any] = None

    def create_cursor(self) -> Any:
        """Create a database cursor with MySQL-safe buffering."""
        if self.connection is None:
            raise RuntimeError("Database connection is not established")

        if self.db_type == "mysql":
            return self.connection.cursor(buffered=True)

        return self.connection.cursor()

    def connect(self) -> bool:
        """Establish database connection.

        Returns:
            True if connection successful, False otherwise
        """
        try:
            if self.db_type == "mysql":
                return self._connect_mysql()
            elif self.db_type == "derby":
                return self._connect_derby()
            else:
                print(f"Unsupported database type: {self.db_type}")
                return False
        except Exception as e:
            print(f"Error connecting to {self.db_type.upper()}: {e}")
            return False

    def _connect_mysql(self) -> bool:
        """Establish MySQL connection.

        Returns:
            True if connection successful
        """
        connect_kwargs: dict[str, Any] = {
            "host": self.host,
            "database": self.database,
            "user": self.user,
            "password": self.password,
        }
        if self.port is not None:
            connect_kwargs["port"] = int(self.port)

        self.connection = mysql.connector.connect(
            **connect_kwargs,
        )
        if self.connection.is_connected():
            print(f"Connected to MySQL database '{self.database}'")
            return True
        return False

    def _connect_derby(self) -> bool:
        """Establish Derby connection.

        Returns:
            True if connection successful

        Raises:
            Exception: If connection fails
        """
        if not jpype.isJVMStarted():
            jpype.startJVM(classpath=[DERBY_JAR_PATH, DERBY_SHARED_JAR_PATH])

        connection_string = f"jdbc:derby://{self.host}:{self.port}/{self.database};create=true"
        driver_class = "org.apache.derby.client.ClientAutoloadedDriver"

        try:
            credentials = [self.user] if not self.password else [self.user, self.password]
            self.connection = jaydebeapi.connect(
                driver_class,
                connection_string,
                credentials,
            )
            print(
                f"Connected to Derby database '{self.database}' using network server "
                f"at {self.host}:{self.port}"
            )
            return True
        except Exception as network_error:
            print(f"Network server connection failed: {network_error}")
            raise network_error

    def disconnect(self) -> None:
        """Close database connection."""
        if self.connection:
            try:
                if self.db_type == "mysql" and self.connection.is_connected():
                    self.connection.close()
                    print("MySQL connection closed")
                elif self.db_type == "derby":
                    self.connection.close()
                    print("Derby connection closed")
            except Exception as e:
                print(f"Error closing connection: {e}")

    def create_table_if_not_exists(self, table_name: str, create_sql: str) -> None:
        """Create table if it doesn't exist, handling database-specific syntax.

        Args:
            table_name: Name of the table to create
            create_sql: CREATE TABLE SQL statement with TABLE_NAME placeholder
        """
        if self.db_type != "derby":
            return

        cursor = self.create_cursor()
        try:
            prefixed_name = f"{self.adapter.get_table_prefix()}{table_name}"
            cursor.execute(create_sql.replace("TABLE_NAME", prefixed_name))
            print(f"Created {table_name} table")
        except Exception as e:
            print(f"{table_name.title()} table creation error (may already exist): {e}")
        finally:
            cursor.close()

    def execute_insert(
        self, table_name: str, columns: List[str], values: List[Any], return_id: bool = True,
        cursor: Any = None
    ) -> Optional[int]:
        """Execute insert query with database-specific parameter handling.

        Args:
            table_name: Target table name
            columns: List of column names
            values: List of values to insert
            return_id: Whether to return the last inserted ID
            cursor: Optional cursor to use (if None, creates new cursor)

        Returns:
            Last inserted ID if return_id is True, None otherwise
        """
        own_cursor = cursor is None
        if own_cursor:
            cursor = self.create_cursor()
        try:
            query = self.adapter.build_insert_query(table_name, columns)
            cursor.execute(query, values)

            if return_id:
                return self.adapter.get_last_insert_id(cursor, table_name)
            return None
        finally:
            if own_cursor:
                cursor.close()

    def execute_query(self, query: str, params: Optional[tuple] = None) -> None:
        """Execute a raw query.

        Args:
            query: SQL query string
            params: Query parameters (optional)
        """
        cursor = self.create_cursor()
        try:
            if params:
                cursor.execute(query, params)
            else:
                cursor.execute(query)
        finally:
            cursor.close()

    def commit(self) -> None:
        """Commit the current transaction."""
        if self.connection:
            self.connection.commit()
