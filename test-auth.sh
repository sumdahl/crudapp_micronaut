#!/bin/bash

# JWT Authentication Test Script
# Tests all authentication endpoints

BASE_URL="http://localhost:8080/api/auth"
COOKIES_FILE="cookies.txt"

echo "========================================="
echo "JWT Authentication Test Script"
echo "========================================="
echo ""

# 1. Test Registration
echo "1. Testing Registration..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }')

echo "Registration Response:"
echo "$REGISTER_RESPONSE" | jq '.' 2>/dev/null || echo "$REGISTER_RESPONSE"
echo ""

# 2. Test Login
echo "2. Testing Login..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }' \
  -c "$COOKIES_FILE")

echo "Login Response:"
echo "$LOGIN_RESPONSE" | jq '.' 2>/dev/null || echo "$LOGIN_RESPONSE"

# Extract access token
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken' 2>/dev/null)
echo ""
echo "Access Token: $ACCESS_TOKEN"
echo ""

# 3. Test /me endpoint with access token
echo "3. Testing /me endpoint (authenticated)..."
ME_RESPONSE=$(curl -s -X GET "$BASE_URL/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Me Response:"
echo "$ME_RESPONSE" | jq '.' 2>/dev/null || echo "$ME_RESPONSE"
echo ""

# 4. Test /me endpoint without token (should fail)
echo "4. Testing /me endpoint (unauthenticated - should fail)..."
ME_UNAUTH_RESPONSE=$(curl -s -w "\nHTTP Status: %{http_code}" -X GET "$BASE_URL/me")
echo "$ME_UNAUTH_RESPONSE"
echo ""

# 5. Test refresh token
echo "5. Testing Refresh Token..."
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/refresh" \
  -b "$COOKIES_FILE" \
  -c "$COOKIES_FILE")

echo "Refresh Response:"
echo "$REFRESH_RESPONSE" | jq '.' 2>/dev/null || echo "$REFRESH_RESPONSE"

# Extract new access token
NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.accessToken' 2>/dev/null)
echo ""
echo "New Access Token: $NEW_ACCESS_TOKEN"
echo ""

# 6. Test logout
echo "6. Testing Logout..."
LOGOUT_RESPONSE=$(curl -s -X POST "$BASE_URL/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -b "$COOKIES_FILE")

echo "Logout Response:"
echo "$LOGOUT_RESPONSE" | jq '.' 2>/dev/null || echo "$LOGOUT_RESPONSE"
echo ""

# 7. Test accessing /me after logout (should fail)
echo "7. Testing /me after logout (should fail)..."
ME_AFTER_LOGOUT=$(curl -s -w "\nHTTP Status: %{http_code}" -X GET "$BASE_URL/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$ME_AFTER_LOGOUT"
echo ""

# Cleanup
rm -f "$COOKIES_FILE"

echo "========================================="
echo "Test Complete!"
echo "========================================="
