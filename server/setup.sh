#!/usr/bin/env bash
# Equinox — turn a fresh Ubuntu/Debian server into your private WireGuard VPN.
#
#   sudo bash setup.sh            # first run: installs + creates your first device ("phone")
#   sudo bash setup.sh add laptop # add another device
#   sudo bash setup.sh show phone # show a device's QR code again
#
set -euo pipefail

WG_DIR=/etc/wireguard
WG_IF=wg0
PORT=${PORT:-51820}
SUBNET=10.66.66
DNS=${DNS:-1.1.1.1, 1.0.0.1}

die() { echo "error: $*" >&2; exit 1; }
[[ $EUID -eq 0 ]] || die "run as root: sudo bash $0 $*"

valid_name() { [[ $1 =~ ^[a-zA-Z0-9_-]{1,15}$ ]] || die "device name must be 1-15 letters, digits, - or _"; }

show_client() {
  local conf="$WG_DIR/clients/$1.conf"
  [[ -f $conf ]] || die "no device named '$1'"
  echo
  echo "──────── $1 ────────  (open Equinox → Servers → Scan QR)"
  qrencode -t ansiutf8 < "$conf"
  echo "Config file: $conf"
  echo
}

add_client() {
  local name=$1; valid_name "$name"
  [[ -f $WG_DIR/clients/$name.conf ]] && die "device '$name' already exists"
  local used next i priv pub psk
  used=$(grep -oP "AllowedIPs = $SUBNET\.\K[0-9]+" "$WG_DIR/$WG_IF.conf" || true)
  next=""
  for i in $(seq 2 254); do grep -qx "$i" <<<"$used" || { next=$i; break; }; done
  [[ -n $next ]] || die "subnet is full"

  priv=$(wg genkey); pub=$(wg pubkey <<<"$priv"); psk=$(wg genpsk)

  cat >> "$WG_DIR/$WG_IF.conf" <<CONF

[Peer]
# $name
PublicKey = $pub
PresharedKey = $psk
AllowedIPs = $SUBNET.$next/32
CONF

  cat > "$WG_DIR/clients/$name.conf" <<CONF
[Interface]
PrivateKey = $priv
Address = $SUBNET.$next/32
DNS = $DNS
MTU = 1420

[Peer]
PublicKey = $(cat "$WG_DIR/server.pub")
PresharedKey = $psk
Endpoint = $(cat "$WG_DIR/endpoint"):$PORT
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
CONF
  chmod 600 "$WG_DIR/clients/$name.conf"

  # Apply without dropping existing connections.
  wg syncconf "$WG_IF" <(wg-quick strip "$WG_IF")
  show_client "$name"
}

install_server() {
  echo "› Installing WireGuard…"
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y -qq wireguard qrencode iptables curl >/dev/null

  local ext_if public_ip
  ext_if=$(ip -4 route show default | awk '{print $5; exit}')
  [[ -n $ext_if ]] || die "could not detect the network interface"
  public_ip=$(curl -4 -fsS https://api.ipify.org || curl -4 -fsS https://ifconfig.me) || die "could not detect public IP"

  mkdir -p "$WG_DIR/clients"; chmod 700 "$WG_DIR"
  umask 077
  wg genkey | tee "$WG_DIR/server.key" | wg pubkey > "$WG_DIR/server.pub"
  echo "$public_ip" > "$WG_DIR/endpoint"

  cat > "$WG_DIR/$WG_IF.conf" <<CONF
[Interface]
Address = $SUBNET.1/24
ListenPort = $PORT
PrivateKey = $(cat "$WG_DIR/server.key")
PostUp = iptables -I INPUT -p udp --dport $PORT -j ACCEPT; iptables -I FORWARD -i %i -j ACCEPT; iptables -I FORWARD -o %i -m state --state RELATED,ESTABLISHED -j ACCEPT; iptables -t nat -A POSTROUTING -s $SUBNET.0/24 -o $ext_if -j MASQUERADE
PostDown = iptables -D INPUT -p udp --dport $PORT -j ACCEPT; iptables -D FORWARD -i %i -j ACCEPT; iptables -D FORWARD -o %i -m state --state RELATED,ESTABLISHED -j ACCEPT; iptables -t nat -D POSTROUTING -s $SUBNET.0/24 -o $ext_if -j MASQUERADE
CONF

  echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-equinox.conf
  sysctl -q --system

  if command -v ufw >/dev/null && ufw status | grep -q active; then
    ufw allow "$PORT/udp" >/dev/null
  fi

  systemctl enable --now "wg-quick@$WG_IF" >/dev/null
  echo "› Server running on $public_ip:$PORT/udp"
  add_client phone
  cat <<MSG
Done. Scan the QR code above with the Equinox app.
Cloud firewall: make sure UDP port $PORT is open for inbound traffic
(Oracle Cloud: VCN → Security List → Add Ingress Rule → UDP $PORT from 0.0.0.0/0).
MSG
}

case ${1:-install} in
  install)
    if [[ -f $WG_DIR/$WG_IF.conf ]]; then
      echo "Already installed. Use: sudo bash $0 add <device>"; exit 0
    fi
    install_server ;;
  add)  add_client "${2:?usage: $0 add <device-name>}" ;;
  show) show_client "${2:?usage: $0 show <device-name>}" ;;
  *)    die "usage: $0 [install | add <device> | show <device>]" ;;
esac
