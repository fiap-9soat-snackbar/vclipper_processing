// MongoDB initialization script for VClipper Processing Service
// This script runs when the MongoDB container starts for the first time

print('🚀 Initializing VClipper Processing Database...');

// Switch to vclipper database
db = db.getSiblingDB('vclipper');

// Create application user for the vclipper database
print('👤 Creating application user...');
db.createUser({
  user: 'vclipperuser',
  pwd: 'vclipper01',
  roles: [
    {
      role: 'readWrite',
      db: 'vclipper'
    }
  ]
});
print('✅ Application user created successfully!');

// Create collections with proper indexing
print('📋 Creating videoProcessingRequests collection...');
db.createCollection('videoProcessingRequests');

// Create indexes for better query performance
print('🔍 Creating indexes...');

// Index on userId for fast user queries
db.videoProcessingRequests.createIndex({ "userId": 1 });

// Index on statusValue for status-based queries
db.videoProcessingRequests.createIndex({ "statusValue": 1 });

// Compound index for user + status queries
db.videoProcessingRequests.createIndex({ "userId": 1, "statusValue": 1 });

// Index on createdAt for sorting (newest first)
db.videoProcessingRequests.createIndex({ "createdAt": -1 });

// Index on updatedAt for monitoring stale requests
db.videoProcessingRequests.createIndex({ "updatedAt": 1 });

// Compound index for retry queries (status + retryCount)
db.videoProcessingRequests.createIndex({ "statusValue": 1, "retryCount": 1 });

print('✅ Database initialization completed!');
print('📊 Created indexes:');
print('   - userId (ascending)');
print('   - statusValue (ascending)');
print('   - userId + statusValue (compound)');
print('   - createdAt (descending)');
print('   - updatedAt (ascending)');
print('   - statusValue + retryCount (compound)');

print('🎯 VClipper Processing Database is ready for use!');
