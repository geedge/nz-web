#!/bin/bash
set -evx
CUR_PWD=`pwd`

echo 'add sys_i18n begin ... '

# 检查 国际化 数据是否完整
C_RESULT=`mysql -h $NZ_DB_HOST -u$NZ_DB_USER -p$NZ_DB_PASSWORD -D $NZ_DB_NAME \
   -NBe "SELECT t.code FROM ( SELECT count( DISTINCT lang ) counter, code FROM sys_i18n GROUP BY code ) t WHERE t.counter = 1;"`

if [ ! -z "$C_RESULT" ]
then
      echo "Sys_i18n loss of language content, please check code:"
      echo "$C_RESULT"
      exit 1
fi

# 导出表数据
mysqldump --extended-insert=FALSE --add-locks=FALSE  -h $NZ_DB_HOST -u$NZ_DB_USER -p$NZ_DB_PASSWORD $NZ_DB_NAME sys_i18n > ./nz-admin/src/main/resources/db/R__AZ_sys_i18n.sql

# 提交 git
git init
git config --global user.email "shizhendong@zdjizhi.com"
git config --global user.name "ci"
git config --global push.default current
git status
git commit nz-admin/src/main/resources/db/R__AZ_sys_i18n.sql -m "[ci skip] fix: ci update sys_i18n data"
git push https://shizhendong:NGzn5d6uymQSGnAsyJa5@git.mesalab.cn/nezha/nz-web.git HEAD:$NZ_COMMIT_BRANCH

echo 'add sys_i18n finished'
exit 0
