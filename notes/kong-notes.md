# API网关kong

## kong的安装与使用

kong的repository安装方式：

```shell
 $ sudo yum update -y
 $ sudo yum install -y wget
 $ wget https://bintray.com/kong/kong-rpm/rpm -O bintray-kong-kong-rpm.repo
 $ export major_version=`grep -oE '[0-9]+\.[0-9]+' /etc/redhat-release | cut -d "." -f1`
 $ sed -i -e 's/baseurl.*/&\/centos\/'$major_version''/ bintray-kong-kong-rpm.repo
 $ sudo mv bintray-kong-kong-rpm.repo /etc/yum.repos.d/
 $ sudo yum update -y
 $ sudo yum install -y kong
 ```

postgreSQL的安装：作为kong的配置数据库

$ yum install https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
$ yum install postgresql12  # 安装客户端
$ yum install postgresql12-server  # 安装服务端
$ /usr/pgsql-12/bin/postgresql-12-setup initdb 初始化并配置自启动
$ systemctl enable postgresql-12
$ systemctl start postgresql-12