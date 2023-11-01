> 本地启动服务，提前验证flyway执行新增sql脚本，是否会报错的方法

1. 更新`bootstrap.yml`，开启flyway

   ![开启flyway](images/开启flyway.png)

2. 更新`application.yml`，更新用户名密码为`root，incloudmanager123456a?`

   ![更新数据库用户名密码](images/更新数据库用户名密码.png)

3. 本地启动服务，flyway会执行新增的sql脚本

   ![本地启动服务flyway报错](images/本地启动服务flyway报错.png)

4. 经测试：正常sql，sql脚本本身有问题，sql文件名有问题，都能覆盖到

<span style=color:red>**！注意：验证完还原`bootstrap.yml`和`application.yml`**</span>