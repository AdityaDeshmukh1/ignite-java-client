#!/bin/bash
# cleanup.sh - Delete all Ignite persistent storage, WAL, and WAL archive files

BASE_DIR="/home/adi-gnome/Personal/Systems-Study/ignite/ignite-java-client/db"

echo "Deleting all files in $BASE_DIR/storage ..."
rm -rf "$BASE_DIR/storage"/*

echo "Deleting all files in $BASE_DIR/wal ..."
rm -rf "$BASE_DIR/wal"/*

echo "Deleting all files in $BASE_DIR/wal-archive ..."
rm -rf "$BASE_DIR/wal-archive"/*

echo "Cleanup complete."

