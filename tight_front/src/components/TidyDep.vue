<template>
    <div class="ibox-content">
        <h1>整理課</h1>
        <ul class="nav nav-tabs">
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
        <br>
        <router-view></router-view>
    </div>
</template>
<style>
    body{
        background-color:#ff0000;
    }
</style>
<script>
    export default{
        data(){
            return{
            }
        },
        components:{
        }
    }
</script>
