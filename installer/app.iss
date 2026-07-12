#ifndef AppSource
  #define AppSource ".\staging\HQTraining"
#endif

#ifndef OutputDir
  #define OutputDir ".\output"
#endif

#define AppId "HQTrainingManagementSystem"
#define AppName "HQ技术培训管理系统"
#define AppVersion "1.0.0"
#define AppPublisher "HQ Training"
#define AppExeName "scripts\\launch_app.bat"

[Setup]
AppId={#AppId}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\HQTraining
DefaultGroupName={#AppName}
OutputDir={#OutputDir}
OutputBaseFilename=HQTrainingSetup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
DisableProgramGroupPage=yes
ChangesAssociations=no
UninstallDisplayName={#AppName}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; GroupDescription: "附加任务:"; Flags: unchecked

[Files]
Source: "{#AppSource}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-ExecutionPolicy Bypass -File ""{app}\scripts\init_db.ps1"""; StatusMsg: "正在初始化私有数据库..."; Flags: runhidden waituntilterminated
Filename: "{app}\{#AppExeName}"; Description: "安装完成后启动 HQ技术培训管理系统"; Flags: nowait postinstall skipifsilent

[Code]
var
  RemoveDataOnUninstall: Boolean;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ResultCode: Integer;
  Params: string;
begin
  if CurUninstallStep = usUninstall then
  begin
    if not UninstallSilent then
    begin
      RemoveDataOnUninstall :=
        (SuppressibleMsgBox('是否在卸载时同时删除本地数据库数据和日志？', mbConfirmation, MB_YESNO, IDNO) = IDYES);
    end
    else
    begin
      RemoveDataOnUninstall := False;
    end;

    Params := '-ExecutionPolicy Bypass -File "' + ExpandConstant('{app}\scripts\uninstall_cleanup.ps1') + '"';
    if RemoveDataOnUninstall then
      Params := Params + ' -RemoveData';

    Exec(
      ExpandConstant('{sys}\WindowsPowerShell\v1.0\powershell.exe'),
      Params,
      ExpandConstant('{app}'),
      SW_HIDE,
      ewWaitUntilTerminated,
      ResultCode
    );
  end;
end;
