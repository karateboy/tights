import Vue from 'vue';
import Vuex from 'vuex';
import authenticated from './moudles/authenticated';
import order from './moudles/order'
import moment from 'moment'
Vue.use(Vuex);
export const store = new Vuex.Store({
    state: {
        defaultFinishDate: moment('0', 'hh')
            .subtract(1, 'days')
            .toDate().getTime()
    },
    getters: {
    },
    mutations: {
        setDefaultFinishDate(state, value) {
            state.defaultFinishDate = value;
        }
    },
    actions: {
    },
    modules: {
        authenticated,
        order
    }
});
