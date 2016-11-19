# libresonic.nsi

!include "WordFunc.nsh"
!include "MUI.nsh"

!insertmacro VersionCompare

# The name of the installer
Name "Libresonic"

# The default installation directory
InstallDir $PROGRAMFILES\Libresonic

# Registry key to check for directory (so if you install again, it will
# overwrite the old one automatically)
InstallDirRegKey HKLM "Software\Libresonic" "Install_Dir"

#--------------------------------
#Interface Configuration

!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Header\orange.bmp"
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\Getting Started.html"
!define MUI_FINISHPAGE_SHOWREADME_TEXT "View Getting Started document"

#--------------------------------
# Pages

# This page checks for JRE
Page custom CheckInstalledJRE

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Languages
!insertmacro MUI_LANGUAGE "English"

Section "Libresonic"

  SectionIn RO

  # Install for all users
  SetShellVarContext "all"

  # Take backup of existing libresonic-service.exe.vmoptions
  CopyFiles /SILENT $INSTDIR\libresonic-service.exe.vmoptions $TEMP\libresonic-service.exe.vmoptions

  # Silently uninstall existing version.
  ExecWait '"$INSTDIR\uninstall.exe" /S _?=$INSTDIR'

  # Remove previous Jetty temp directory.
  RMDir /r "c:\libresonic\jetty"

  # Backup database.
  RMDir /r "c:\libresonic\db.backup"
  CreateDirectory "c:\libresonic\db.backup"
  CopyFiles /SILENT "c:\libresonic\db\*" "c:\libresonic\db.backup"

  # Set output path to the installation directory.
  SetOutPath $INSTDIR

  # Write files.
  File ..\..\..\target\libresonic-agent.exe
  File ..\..\..\target\libresonic-agent.exe.vmoptions
  File ..\..\..\target\libresonic-agent-elevated.exe
  File ..\..\..\target\libresonic-agent-elevated.exe.vmoptions
  File ..\..\..\target\libresonic-service.exe
  File ..\..\..\target\libresonic-service.exe.vmoptions
  File ..\..\..\..\libresonic-booter\target\libresonic-booter-jar-with-dependencies.jar
  File ..\..\..\..\libresonic-main\README.TXT
  File ..\..\..\..\libresonic-main\LICENSE.TXT
  File "..\..\..\..\libresonic-main\Getting Started.html"
  File ..\..\..\..\libresonic-main\target\libresonic.war
  File ..\..\..\..\libresonic-main\target\classes\version.txt
  File ..\..\..\..\libresonic-main\target\classes\build_number.txt

  # Write the installation path into the registry
  WriteRegStr HKLM SOFTWARE\Libresonic "Install_Dir" "$INSTDIR"

  # Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Libresonic" "DisplayName" "Libresonic"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Libresonic" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Libresonic" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Libresonic" "NoRepair" 1
  WriteUninstaller "uninstall.exe"

  # Restore libresonic-service.exe.vmoptions
  CopyFiles /SILENT  $TEMP\libresonic-service.exe.vmoptions $INSTDIR\libresonic-service.exe.vmoptions
  Delete $TEMP\libresonic-service.exe.vmoptions

  # Write transcoding pack files.
  SetOutPath "c:\libresonic\transcode"
  File ..\..\..\..\libresonic-transcode\windows\*.*

  # Add Windows Firewall exception.
  # (Requires NSIS plugin found on http://nsis.sourceforge.net/NSIS_Simple_Firewall_Plugin to be installed
  # as NSIS_HOME/Plugins/SimpleFC.dll)

  SimpleFC::AdvAddRule "Libresonic Service (TCP)" "" "6" "1" "1" "7" "1" "$INSTDIR\libresonic-service.exe" "" "" "Libresonic" "" "" "" ""
  SimpleFC::AdvAddRule "Libresonic Service (UDP)" "" "17" "1" "1" "7" "1" "$INSTDIR\libresonic-service.exe" "" "" "Libresonic" "" "" "" ""
  SimpleFC::AdvAddRule "Libresonic Agent (TCP)" "" "6" "1" "1" "7" "1" "$INSTDIR\libresonic-agent.exe" "" "" "Libresonic" "" "" "" ""
  SimpleFC::AdvAddRule "Libresonic Agent (UDP)" "" "17" "1" "1" "7" "1" "$INSTDIR\libresonic-agent.exe" "" "" "Libresonic" "" "" "" ""
  SimpleFC::AdvAddRule "Libresonic Agent Elevated (TCP)" "" "6" "1" "1" "7" "1" "$INSTDIR\libresonic-agent-elevated.exe" "" "" "Libresonic" "" "" "" ""
  SimpleFC::AdvAddRule "Libresonic Agent Elevated (UDP)" "" "17" "1" "1" "7" "1" "$INSTDIR\libresonic-agent-elevated.exe" "" "" "Libresonic" "" "" "" ""

  # Install and start service.
  ExecWait '"$INSTDIR\libresonic-service.exe" -install'
  ExecWait '"$INSTDIR\libresonic-service.exe" -start'

  # Start agent.
  Exec '"$INSTDIR\libresonic-agent-elevated.exe" -balloon'

SectionEnd


Section "Start Menu Shortcuts"

  CreateDirectory "$SMPROGRAMS\Libresonic"
  CreateShortCut "$SMPROGRAMS\Libresonic\Open Libresonic.lnk"          "$INSTDIR\libresonic.url"         ""         "$INSTDIR\libresonic-agent.exe"  0
  CreateShortCut "$SMPROGRAMS\Libresonic\Libresonic Tray Icon.lnk"     "$INSTDIR\libresonic-agent.exe"   "-balloon" "$INSTDIR\libresonic-agent.exe"  0
  CreateShortCut "$SMPROGRAMS\Libresonic\Start Libresonic Service.lnk" "$INSTDIR\libresonic-service.exe" "-start"   "$INSTDIR\libresonic-service.exe"  0
  CreateShortCut "$SMPROGRAMS\Libresonic\Stop Libresonic Service.lnk"  "$INSTDIR\libresonic-service.exe" "-stop"    "$INSTDIR\libresonic-service.exe"  0
  CreateShortCut "$SMPROGRAMS\Libresonic\Uninstall Libresonic.lnk"     "$INSTDIR\uninstall.exe"        ""         "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\Libresonic\Getting Started.lnk"        "$INSTDIR\Getting Started.html" ""         "$INSTDIR\Getting Started.html" 0

  CreateShortCut "$SMSTARTUP\Libresonic.lnk"                         "$INSTDIR\libresonic-agent.exe"   ""         "$INSTDIR\libresonic-agent.exe"  0

SectionEnd


# Uninstaller

Section "Uninstall"

  # Uninstall for all users
  SetShellVarContext "all"

  # Stop and uninstall service if present.
  ExecWait '"$INSTDIR\libresonic-service.exe" -stop'
  ExecWait '"$INSTDIR\libresonic-service.exe" -uninstall'

  # Stop agent by killing it.
  # (Requires NSIS plugin found on http://nsis.sourceforge.net/Processes_plug-in to be installed
  # as NSIS_HOME/Plugins/Processes.dll)
  Processes::KillProcess "libresonic-agent"
  Processes::KillProcess "libresonic-agent-elevated"
  Processes::KillProcess "ffmpeg"

  # Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Libresonic"
  DeleteRegKey HKLM SOFTWARE\Libresonic

  # Remove files.
  Delete "$SMSTARTUP\Libresonic.lnk"
  RMDir /r "$SMPROGRAMS\Libresonic"
  Delete "$INSTDIR\build_number.txt"
  Delete "$INSTDIR\elevate.exe"
  Delete "$INSTDIR\Getting Started.html"
  Delete "$INSTDIR\LICENSE.TXT"
  Delete "$INSTDIR\README.TXT"
  Delete "$INSTDIR\libresonic.url"
  Delete "$INSTDIR\libresonic.war"
  Delete "$INSTDIR\libresonic-agent.exe"
  Delete "$INSTDIR\libresonic-agent.exe.vmoptions"
  Delete "$INSTDIR\libresonic-agent-elevated.exe"
  Delete "$INSTDIR\libresonic-agent-elevated.exe.vmoptions"
  Delete "$INSTDIR\libresonic-booter-jar-with-dependencies.jar"
  Delete "$INSTDIR\libresonic-service.exe"
  Delete "$INSTDIR\libresonic-service.exe.vmoptions"
  Delete "$INSTDIR\uninstall.exe"
  Delete "$INSTDIR\version.txt"
  RMDir /r "$INSTDIR\log"
  RMDir "$INSTDIR"

  # Remove Windows Firewall exception.
  # (Requires NSIS plugin found on http://nsis.sourceforge.net/NSIS_Simple_Firewall_Plugin to be installed
  # as NSIS_HOME/Plugins/SimpleFC.dll)
  SimpleFC::AdvRemoveRule "Libresonic Service (TCP)"
  SimpleFC::AdvRemoveRule "Libresonic Service (UDP)"
  SimpleFC::AdvRemoveRule "Libresonic Agent (TCP)"
  SimpleFC::AdvRemoveRule "Libresonic Agent (UDP)"
  SimpleFC::AdvRemoveRule "Libresonic Agent Elevated (TCP)"
  SimpleFC::AdvRemoveRule "Libresonic Agent Elevated (UDP)"

SectionEnd


Function CheckInstalledJRE
    # Read the value from the registry into the $0 register
    ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" CurrentVersion

    # Check JRE version. At least 1.6 is required.
    #   $1=0  Versions are equal
    #   $1=1  Installed version is newer
    #   $1=2  Installed version is older (or non-existent)
    ${VersionCompare} $0 "1.6" $1
    IntCmp $1 2 InstallJRE 0 0
    Return

    InstallJRE:
      # Launch Java web installer.
      MessageBox MB_OK "Java was not found and will now be installed."
      File /oname=$TEMP\jre-setup.exe jre-8u31-windows-i586-iftw.exe
      ExecWait '"$TEMP\jre-setup.exe"' $0
      Delete "$TEMP\jre-setup.exe"

FunctionEnd
