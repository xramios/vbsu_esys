#!/usr/bin/env python3
"""Room seeder for the College of Engineering building."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from seeder.core.database import DatabaseManager


@dataclass(frozen=True, slots=True)
class RoomSeedSummary:
    """Summary of room seeding actions."""

    rooms_created: int
    rooms_skipped: int


class RealRoomSeeder:
    """Seeds rooms for the College of Engineering building.

    Room numbering pattern:
    - First floor: EN100 - EN116
    - Second floor: EN200 - EN216
    - Third floor: EN300 - EN316
    - Fourth floor: EN400 - EN416
    - Computer Rooms: CLR1 - CLR5

    All rooms have 40 capacity.
    """

    DEFAULT_CAPACITY = 40
    BUILDING_NAME = "College of Engineering"

    def __init__(self, db_manager: DatabaseManager) -> None:
        self.db_manager = db_manager
        self._table_prefix = "APP." if db_manager.db_type == "derby" else ""

    def seed(self) -> RoomSeedSummary:
        """Seed all rooms for the College of Engineering building."""
        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.create_cursor()
        try:
            existing_rooms = self._load_existing_rooms(cursor)
            rooms_created = 0
            rooms_skipped = 0

            room_codes = self._generate_room_codes()

            for room_code in room_codes:
                if room_code in existing_rooms:
                    rooms_skipped += 1
                    continue

                if room_code.startswith("CLR"):
                    room_type = "LAB"
                else:
                    room_type = "LECTURE"

                self.db_manager.execute_insert(
                    "rooms",
                    ["building", "room_type", "status", "room", "capacity"],
                    [self.BUILDING_NAME, room_type, "AVAILABLE", room_code, self.DEFAULT_CAPACITY],
                    return_id=False,
                    cursor=cursor,
                )
                rooms_created += 1

            self.db_manager.commit()
            return RoomSeedSummary(
                rooms_created=rooms_created,
                rooms_skipped=rooms_skipped,
            )
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _generate_room_codes(self) -> list[str]:
        """Generate all room codes for the College of Engineering building."""
        rooms: list[str] = []

        # First floor: EN100 - EN116
        for i in range(17):
            rooms.append(f"EN{100 + i}")

        # Second floor: EN200 - EN216
        for i in range(17):
            rooms.append(f"EN{200 + i}")

        # Computer Rooms: CLR1 - CLR5
        for i in range(1, 6):
            rooms.append(f"CLR{i}")

        # Third floor: EN300 - EN316
        for i in range(17):
            rooms.append(f"EN{300 + i}")

        # Fourth floor: EN400 - EN416
        for i in range(17):
            rooms.append(f"EN{400 + i}")

        return rooms

    def _load_existing_rooms(self, cursor: Any) -> set[str]:
        """Load existing room codes from the database."""
        query = f"SELECT room FROM {self._table('rooms')}"
        cursor.execute(query)
        rooms: set[str] = set()
        for row in cursor.fetchall():
            if row[0] is not None:
                rooms.add(str(row[0]).strip().upper())
        return rooms

    def _table(self, table_name: str) -> str:
        return f"{self._table_prefix}{table_name}"
