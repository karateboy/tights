<template>
    <div class="sidebar-collapse">
        <ul class="nav metismenu" id="side-menu">
            <li class="nav-header">
                <div class="dropdown profile-element">
                    <a data-toggle="dropdown" class="dropdown-toggle" href="#">
                            <span class="clear"> <span class="block m-t-xs"> <strong
                                    class="font-bold">{{ user.name }}</strong>
                             </span> <span class="text-muted text-xs block">{{ groupInfoMap[user.groupId]}} <b
                                    class="caret"></b></span> </span> </a>
                    <ul class="dropdown-menu animated fadeInRight m-t-xs">
                        <li><a href="#">Logout</a></li>
                    </ul>
                </div>
                <div class="logo-element">
                    IN+
                </div>
            </li>
            <router-link tag="li" to="/" active-class="active" exact><a><i class="fa fa-th-large"></i> <span
                    class="nav-label">儀錶板</span></a></router-link>
            <li>
                <a><i class="fa fa-th-large"></i> <span class="nav-label">訂單管理</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li" to="/Order/New" active-class="active"><a><i class="fa fa-th-large"></i> <span
                            class="nav-label">新增訂單</span> </a></router-link>
                    <router-link tag="li" to="/Order/Mine" active-class="active"><a><i class="fa fa-th-large"></i> <span
                            class="nav-label">我的訂單</span> </a></router-link>
                    <router-link tag="li" to="/Order/Query" active-class="active"><a><i class="fa fa-th-large"></i>
                        <span
                                class="nav-label">查詢訂單</span> </a></router-link>
                </ul>
            </li>
            <li>
                <a><i class="fa fa-th-large"></i> <span class="nav-label">生產排程</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link to='/Schedule/New' tag='li' role="presentation" active-class='active'><a>排定工作</a>
                    </router-link>
                    <router-link to='/Schedule/ActiveDyeCardList' tag='li' role="presentation" active-class='active'><a>處理中漂染單</a>
                    </router-link>
                    <router-link to='/Schedule/QueryDyeCard' tag='li' role="presentation" active-class='active'>
                        <a>查詢漂染單</a>
                    </router-link>
                    <router-link to='/Schedule/WorkCard' tag='li' role="presentation" active-class='active'>
                        <a>處理中流動卡</a>
                    </router-link>
                    <router-link to='/Schedule/QueryWorkCard' tag='li' role="presentation" active-class='active'><a>查詢流動卡</a>
                    </router-link>
                </ul>
            </li>
            <li>
                <a><i class="fa fa-th-large"></i> <span class="nav-label">漂染課</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li" :to="{name:'StartDye'}" active-class="active"><a>
                        <i class="fa fa-th-large"></i>
                        <span class="nav-label">開始漂染</span> </a></router-link>
                    <router-link tag="li" :to="{name:'EndDye'}" active-class="active"><a>
                        <i class="fa fa-th-large"></i>
                        <span class="nav-label">漂染結束</span> </a></router-link>
                    <router-link tag="li" :to="{name:'UpdateDyeCard'}" active-class="active"><a>
                        <i class="fa fa-th-large"></i>
                        <span class="nav-label">更新漂染卡</span> </a></router-link>
                </ul>
            </li>
            <li>
                <a><i class="fa fa-th-large"></i> <span class="nav-label">定型課</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li" :to="{name:'UpdateStylingCard'}" active-class="active"><a>
                        <i class="fa fa-th-large"></i>
                        <span class="nav-label">定型</span> </a></router-link>
                    <router-link tag="li" :to="{name:'StylingReport'}" active-class="active"><a>
                        <i class="fa fa-th-large"></i>
                        <span class="nav-label">報表</span> </a></router-link>
                </ul>
            </li>
            <li>
                <a><i class="fa fa-th-large"></i> <span class="nav-label">整理課</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li"
                                 :to="{name:'UpdateTidyCard',
                                 params:{ phase:'檢襪'}}" active-class="active">
                        <a><i class="fa fa-th-large"></i>
                            <span class="nav-label">檢襪</span> </a></router-link>
                    <router-link tag="li"
                                 :to="{name:'UpdateTidyCard',
                                 params:{ phase:'車洗標'}}" active-class="active">
                        <a><i class="fa fa-th-large"></i>
                            <span class="nav-label">車洗標</span> </a></router-link>
                    <router-link tag="li"
                                 :to="{name:'UpdateTidyCard',
                                 params:{ phase:'剪線頭'}}" active-class="active">
                        <a><i class="fa fa-th-large"></i>
                            <span class="nav-label">剪線頭</span> </a></router-link>
                    <router-link tag="li"
                                 :to="{name:'UpdateTidyCard',
                                 params:{ phase:'整理包裝'}}" active-class="active">
                        <a><i class="fa fa-th-large"></i>
                            <span class="nav-label">整理包裝</span> </a></router-link>
                    <router-link tag="li"
                                 :to="{name:'UpdateTidyCard',
                                 params:{ phase:'成品倉庫'}}" active-class="active">
                        <a><i class="fa fa-th-large"></i>
                            <span class="nav-label">成品倉庫</span> </a></router-link>
                    <router-link tag="li" :to="{name:'TidyReport'}" active-class="active"><a>
                        <i class="fa fa-th-large"></i>
                        <span class="nav-label">報表</span> </a></router-link>
                </ul>
            </li>
            <li>
                <a><i class="fa fa-th-large"></i> <span class="nav-label">系統管理</span> <span
                        class="fa arrow"></span></a>
                <ul class="nav nav-second-level collapse">
                    <router-link tag="li"
                                 :to="{name:'AddUser'}" active-class="active">
                        <a><i class="fa fa-th-large"></i>
                            <span class="nav-label">新增使用者</span> </a></router-link>

                    <router-link tag="li"
                                 :to="{name:'DelUser'}" active-class="active">
                        <a><i class="fa fa-th-large"></i>
                            <span class="nav-label">刪除使用者</span> </a></router-link>

                    <router-link tag="li"
                                 :to="{name:'UpdateUser'}" active-class="active">
                        <a><i class="fa fa-th-large"></i>
                            <span class="nav-label">更新使用者</span> </a></router-link>
                </ul>
            </li>
        </ul>
    </div>
</template>
<style scoped>

</style>
<script>
    import {mapGetters} from 'vuex'
    import axios from 'axios'
    export default{
        data(){
            axios.get('/Group').then((resp) => {
                const ret = resp.data
                for (let groupInfo of ret) {
                    this.groupInfoMap[groupInfo.id] = groupInfo.name
                }
            })
            return {
                groupInfoMap: {}
            }
        },
        computed: {
            ...mapGetters(['user'])
        }
    }


</script>
