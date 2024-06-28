### Teamparty
Teamparty 是一個用於 Minecraft 的插件，基於 Spigot 1.20.1 開發，提供了隊伍管理功能。插件允許玩家創建隊伍、邀請成員、設置隊長、管理隊伍狀態以及處理成員離線等功能。

### 特點
* 創建和管理隊伍
* 邀請玩家加入隊伍並設置邀請過期時間
* 設置隊伍最大成員數量限制
* 自動處理斷線玩家並設置斷線時間限制
* 圖形介面（GUI）顯示隊伍狀態
* 提供 API 供其他插件調用
### 安裝
1. 下載最新版本的 Teamparty 插件並將其放入你的 Spigot 伺服器的 plugins 目錄中。
2. 啟動伺服器，插件將自動生成配置文件和消息文件。
3. 配置 config.yml 和 messages.yml 以自定義插件行為和訊息。

### 使用
## 指令
* /team create <隊伍名稱> - 創建隊伍
* /team add <玩家名> - 添加成員
* /team leave - 離開隊伍
* /team gui - 查看隊伍狀態
* /team accept - 接受隊伍邀請
* /team reject - 拒絕隊伍邀請
## 權限
* teamparty.use - 使用插件指令的權限
## API
其他插件可以通過 Bukkit 服務管理器調用 TeamAPI。以下是示例代碼：
```java
TeamAPI teamAPI = Bukkit.getServicesManager().getRegistration(TeamAPI.class).getProvider();

// 示例：檢查玩家是否在隊伍中
Player player = ...; // 獲取玩家對象
boolean isInTeam = teamAPI.isPlayerInTeam(player);

// 示例：獲取玩家的隊伍
Team playerTeam = teamAPI.getPlayerTeam(player);
```

### 可用方法
* boolean isPlayerInTeam(Player player) - 判斷玩家是否在一個隊伍中。
* boolean areMembersNearby(Player player, double radius) - 判斷玩家附近是否有隊伍成員。
* Team getPlayerTeam(Player player) - 獲取玩家所屬的隊伍。
* void disbandTeam(Team team) - 解散指定的隊伍。
* void createTeam(String teamName, Player leader) - 創建一個新的隊伍並設置隊長。
* void addPlayerToTeam(Player player, Team team) - 將玩家添加到指定的隊伍。
* void removePlayerFromTeam(UUID playerId) - 從隊伍中移除指定的玩家。
* List<Team> getAllTeams() - 獲取所有隊伍的列表。
* Set<Player> getTeamMembers(Team team) - 獲取指定隊伍的所有成員。
* void changeTeamLeader(Team team, Player newLeader) - 更改隊伍的隊長。
* String getTeamName(Team team) - 獲取指定隊伍的名稱。
* List<String> getTeamMemberNames(Team team) - 獲取指定隊伍的所有成員名稱。
* boolean isLeader(Player player) - 判斷玩家是否是隊長。
* int getTeamSize(Team team) - 獲取指定隊伍的成員數量。
* void checkDisconnectedPlayers() - 檢查並處理斷線超過指定時間的玩家。
