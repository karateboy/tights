<template>
    <div class="ibox flot-e-margins">
        <div class="ibox-content">
            <ul class="nav nav-tabs">
                <router-link to='/Schedule/New' tag='li' role="presentation" active-class='active'><a>排定工作</a>
                </router-link>
                <router-link to='/Schedule/ActiveDyeCardList' tag='li' role="presentation" active-class='active'><a>處理中漂染單</a>
                </router-link>
                <router-link to='/Schedule/QueryDyeCard' tag='li' role="presentation" active-class='active'><a>查詢漂染單</a>
                </router-link>
                <router-link to='/Schedule/WorkCard' tag='li' role="presentation" active-class='active'><a>處理中流動卡</a>
                </router-link>
                <router-link to='/Schedule/QueryWorkCard' tag='li' role="presentation" active-class='active'><a>查詢流動卡</a>
                </router-link>
            </ul>
            <router-view></router-view>
        </div>
    </div>
</template>
<style scoped>
    .order {
        background-color:#0000ff;
    }
</style>
<script>
    export default{
        data(){
            return {
                msg: '生產排程'
            }
        },
        components: {}
    }
</script>
