import socket
import uuid
import ipaddress
import requests
import pymysql

DB_HOST = 'example.com'
DB_USER = 'user'
DB_PASSWORD = 'eXamp!ePassC0de'
DB_NAME = 'example_db'
DB_PORT = 25566

def get_mac_address():
    mac = uuid.getnode()
    mac_address = ':'.join(format((mac >> i) & 0xff, '02x') for i in range(0, 8 * 6, 8))
    return mac_address

def get_ip_address():
    try:
        hostname = socket.gethostname()
        ip_address = socket.gethostbyname(hostname)
        return ip_address
    except Exception as e:
        print(f"無法獲取內部 IP 地址: {e}")
        return "未知"

def get_public_ip():
    try:
        response = requests.get("https://api64.ipify.org?format=json", timeout=5)
        response.raise_for_status()
        public_ip = response.json()["ip"]
        return public_ip
    except Exception as e:
        print(f"無法獲取公共 IP 地址: {e}")
        return "未知"

def get_device_info():
    try:
        mac_address = get_mac_address() or "未知"
        ip_address = get_ip_address() or "未知"
        public_ip = get_public_ip() or "未知"

        print(f"MAC 地址: {mac_address}")
        print(f"內部 IP 地址: {ip_address}")
        print(f"公共 IP 地址: {public_ip}")

        return {
            "mac_address": mac_address,
            "ip_address": ip_address,
            "public_ip": public_ip
        }
    except Exception as e:
        print(f"獲取設備信息失敗: {e}")
        return {
            "mac_address": "未知 MAC 地址",
            "ip_address": "未知內部 IP",
            "public_ip": "未知公共 IP"
        }

def is_authorized_user():
    current_mac = get_mac_address()
    current_ip = get_ip_address()
    current_public = get_public_ip()

    try:
        connection = pymysql.connect(
            host=DB_HOST,
            user=DB_USER,
            password=DB_PASSWORD,
            database=DB_NAME,
            port=DB_PORT
        )
        cursor = connection.cursor()
        query = """
        SELECT * FROM deviceinfo
        WHERE MACAddress = %s AND PublicIP = %s
        """
        cursor.execute(query, (current_mac, current_public))
        result = cursor.fetchone()
        cursor.close()
        connection.close()
        return bool(result)
    except Exception as e:
        print(f"數據庫操作失敗: {e}")
        return False
