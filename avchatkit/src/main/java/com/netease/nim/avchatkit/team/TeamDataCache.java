package com.netease.nim.avchatkit.team;

import android.util.Log;

import com.netease.nim.avchatkit.common.log.LogUtil;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.team.TeamService;
import com.netease.nimlib.sdk.team.TeamServiceObserver;
import com.netease.nimlib.sdk.team.model.Team;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nanyi on 2020/5/28.
 */

public class TeamDataCache {

    private static final String TEAM_TAG =  "TEAM_TAG";

    private static TeamDataCache instance;

    public static synchronized TeamDataCache getInstance() {
        if (instance == null) {
            instance = new TeamDataCache();
        }

        return instance;
    }

    public void buildCache(boolean async) {
        if(async) {
            NIMClient.getService(TeamService.class).queryTeamList().setCallback(new RequestCallbackWrapper<List<Team>>() {
                @Override
                public void onResult(int code, List<Team> result, Throwable exception) {
                    if(code== ResponseCode.RES_SUCCESS && result!=null ) {
                        Log.i(TEAM_TAG,"set TeamDataCache async     team size:"+result.size());
                        addOrUpdateTeam(result);
                        if(observer!=null)observer.onEvent(result);
                    } else {
                        Log.i(TEAM_TAG,"set TeamDataCache async     code:"+code  +"    result null:"+(result==null));
                    }
                }
            });
        } else {
            final List<Team> teams = NIMClient.getService(TeamService.class).queryTeamListBlock();
            if (teams == null) {
                return;
            }
            Log.i(TEAM_TAG,"set TeamDataCache block     team size:"+teams.size());
            addOrUpdateTeam(teams);
        }
    }


    public void clear() {
        clearTeamCache();
//        clearTeamMemberCache();
    }


    public void addOrUpdateTeam(Team team) {
        if (team == null) {
            return;
        }

        id2TeamMap.put(team.getId(), team);
    }

    private void addOrUpdateTeam(List<Team> teamList) {
        if (teamList == null || teamList.isEmpty()) {
            return;
        }

        for (Team t : teamList) {
            if (t == null) {
                continue;
            }

            id2TeamMap.put(t.getId(), t);
        }
    }



    /**
     * *
     * ******************************************** 群资料缓存 ********************************************
     */

    private Map<String, Team> id2TeamMap = new ConcurrentHashMap<>();

    public void clearTeamCache() {
        id2TeamMap.clear();
    }






    /**
     * *
     * ******************************************** 观察者 ********************************************
     */

    @Deprecated
    public void registerObservers(boolean register) {
//        NIMClient.getService(TeamServiceObserver.class).observeTeamUpdate(teamUpdateObserver, register);
//        NIMClient.getService(TeamServiceObserver.class).observeTeamRemove(teamRemoveObserver, register);
//        NIMClient.getService(TeamServiceObserver.class).observeMemberUpdate(memberUpdateObserver, register);
//        NIMClient.getService(TeamServiceObserver.class).observeMemberRemove(memberRemoveObserver, register);
    }


    // 群资料变动观察者通知。新建群和群更新的通知都通过该接口传递
    private Observer<List<Team>> teamUpdateObserver = new Observer<List<Team>>() {
        @Override
        public void onEvent(final List<Team> teams) {
            if (teams == null) {
                return;
            }
            Log.i(TEAM_TAG,"team update size:" + teams.size());
            addOrUpdateTeam(teams);
        }
    };



    public void registerObserver( Observer<List<Team>> observer) {
        this.observer = observer;
        NIMClient.getService(TeamServiceObserver.class).observeTeamUpdate(observer, true);
    }

    private Observer<List<Team>> observer;


}
