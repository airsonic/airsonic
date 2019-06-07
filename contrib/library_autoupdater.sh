#!/usr/bin/env bash

set -u

info() {
  echo "$(date -Iseconds) - $*"
}

error() {
  info >&2 "$*"
  exit 1
}


# Make sure only root can run our script
(( $(id -u) == 0 )) || error "this script must be run as root!"

# COMMANDS
typeset -r cmd_which="/usr/bin/which" && [[ -x $cmd_which ]] || error "which command not found"
typeset -r cmd_curl=$($cmd_which curl) && [[ -x $cmd_curl ]] || error "curl command not found"
typeset -r cmd_inotifywait=$($cmd_which inotifywait) && [[ -x $cmd_inotifywait ]] || error "inotifywait command not found"
typeset -r cmd_md5sum=$($cmd_which md5sum) && [[ -x $cmd_md5sum ]] || error "md5sum command not found"
typeset -r cmd_awk=$($cmd_which awk) && [[ -x $cmd_awk ]] || error "awk command not found"
typeset -r cmd_openssl=$($cmd_which openssl) && [[ -x $cmd_openssl ]] || error "openssl command not found"

# VARS
typeset -r dirs="/var/music"
typeset -r username="admin"

# typeset -r password=""
# typeset -r salt="$($cmd_openssl rand -hex 20)"
# typeset -r token="$(echo -n "${password}${salt}" | $cmd_md5sum | $cmd_awk '{ print $1 }')"

typeset -r salt=""
typeset -r token=""
typeset -r server=""
typeset -r client="scan-trigger"

trigger_scan() {
  typeset -r url="${server}/rest/startScan?u=${username}&t=${token}&s=${salt}&v=1.15.0&c=${client}"

  response=$($cmd_curl -s $url)
  echo $response | grep -q 'status="ok"' || error "Scan request failed"
  echo $response | grep -q 'scanning="false"' || info "Server is already scanning"
}

info "Start watching dirs '$dirs'"
info "Press CTRL+C to stop..."
for (( ; ; )) do
  $cmd_inotifywait -r -qq \
    -e modify \
    -e move \
    -e create \
    -e delete \
    $dirs

  info "Event received, waiting 10 minutes before triggering new scan"
  sleep 3600 # Wait 10 minutes before triggering the scan
  trigger_scan
  sleep 5
done

# EOF
