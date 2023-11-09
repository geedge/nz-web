package com.nis.modules.election.dao;

import cn.hutool.db.handler.StringHandler;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.common.utils.SpringContextUtils;
import com.nis.common.utils.Tool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LeaderElectionDao {

    private static final Log log = Log.get();

    private static final String ATTEMPT_LEADERSHIP = "insert ignore into sys_leader_election(election_key, leader_id, laset_active_ts) values (?, ?, unix_timestamp() * 1000) on duplicate key update leader_id = if( laset_active_ts < unix_timestamp() * 1000 - ?, values (leader_id), leader_id), laset_active_ts = if( leader_id = values (leader_id), values (laset_active_ts), laset_active_ts );";
    private static final String FORCE_LEADERSHIP = "replace into sys_leader_election(election_key, leader_id, laset_active_ts) values (?, ?, unix_timestamp() * 1000);";
    private static final String FORCE_RE_ELECTION = "delete from sys_leader_election";
    private static final String IS_LEADER = "select count(*) as is_leader from sys_leader_election where election_key = ? and leader_id = ?;";
    private static final String WHO_IS_THE_LEADER = "select leader_id as leader from sys_leader_election where election_key = ?;";

    private static DataSource dataSource;
    private static LeaderElectionDao electionDao;

    private LeaderElectionDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static LeaderElectionDao getInstance() {
        if (electionDao == null) {
            synchronized (LeaderElectionDao.class) {
                if (electionDao == null) {
                    dataSource = SpringContextUtils.getBean("dataSource", DataSource.class);
                    electionDao = new LeaderElectionDao(dataSource);
                }
            }
        }
        return electionDao;
    }

    public void attemptLeadership() {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(ATTEMPT_LEADERSHIP);) {
            SqlExecutor.execute(ps, Constant.SYS_HA_LOCK, Constant.SERVER_ID, 10 * 1000);
        } catch (SQLException e) {
            log.error(e, "[attemptLeadership] [error]");
        }
    }

    public void forceLeadership() {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(FORCE_LEADERSHIP);
        ) {
            SqlExecutor.execute(ps, Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        } catch (SQLException e) {
            log.error(e, "[forceLeadership] [error]");
        }
    }

    public void forceReElection() {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(FORCE_RE_ELECTION);
             ResultSet ignored = ps.executeQuery()) {
        } catch (SQLException e) {
            log.error(e, "[forceReElection] [error]");
        }
    }

    public boolean isLeader() {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(IS_LEADER);) {
            String query = SqlExecutor.query(ps, StringHandler.create(), Constant.SYS_HA_LOCK, Constant.SERVER_ID);
            return Tool.StrUtil.equals("1", query);
        } catch (SQLException e) {
            log.error(e, "[isLeader] [error]");
        }
        return false;
    }

    public String getLeader() {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(WHO_IS_THE_LEADER);) {
            String query = SqlExecutor.query(ps, StringHandler.create(), Constant.SYS_HA_LOCK, Constant.SERVER_ID);
            return query;
        } catch (SQLException e) {
            log.error(e, "[getLeader] [error]");
        }
        return null;
    }

}