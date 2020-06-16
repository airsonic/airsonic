#!/usr/bin/env fish

# Put this in your .config/fish/functions folder and you'll get an `airsonic`
# command that you can use to interact with the rest api. If you just want to
# play around with it, you can execute it directly.

function _airsonic_usage
  echo 'Usage: airsonic [-h|--help] -u $user -p $pass [$url] $endpoint [...$params]'
  echo
  echo 'Interact with an airsonic server via REST. See http://www.subsonic.org/pages/api.jsp for additional documentation.'
  echo
  echo 'If you do not provide a $url, it is assumed the url can be found in $AIRSONIC_URL.'
  echo 'Anything you pass in as $params will be directly passed on to httpie (see https://httpie.org/doc#querystring-parameters).'
  echo
  echo 'Parameters:'
  echo
  echo '  -h, --help	Prints this message'
  echo '  -u, --user	Airsonic user'
  echo '  -p, --pass	The user\'s password'
end

function airsonic -d "Convenience helper for interacting with an airsonic / subsonic server"
  argparse "h/help" "u/user=" "p/pass=" -- $argv

  if test (count $argv) -eq 0 -o (count $_flag_help) -eq 1
    # print short usage description
    _airsonic_usage
    return 0
  end

  if not command -qs http
    # check for dependencies
    echo 'Aborting: Please install httpie. See https://httpie.org/ for instructions.'
    return 1
  end

  if begin test -n "$AIRSONIC_URL"; and not string match -qr '^http' $argv[1]; end
    # if we have an $AIRSONIC_URL and don't pass in a URL explicitly, use that
    set url $AIRSONIC_URL
  else
    # otherwise use the first argument
    set url $argv[1]
    set argv $argv[2..(count $argv)]
  end

  if not string match -qr '^http' $url
    # basic sanity checking
    echo 'Please provide the URL of your airsonic instance as the first argument or set it as $AIRSONIC_URL'
    return 1
  end

  set endpoint $argv[1]
  if test (count $argv) -gt 1
    set params $argv[2..(count $argv)]
  end

  http --follow -b $url/rest/$endpoint v==1.15.0 f==json u==$_flag_user  p==$_flag_pass c==airsonic-httpie $params
end

# execute command directly when not sourced
if test (status current-command) != "source"
  airsonic $argv
end
