package com.irif.projet.genielogiciel.jetty_jersey.DAO.implement;


import com.irif.projet.genielogiciel.jetty_jersey.DAO.DAO;
import com.irif.projet.genielogiciel.jetty_jersey.model.User;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;

public class UserDAO extends DAO<User>{

	
	public UserDAO(TransportClient client){
		super(client);
	}
	/**
	* 
    * @param index type User
	* @return  SearchResponse
	*/
	public SearchResponse getSearchResponse(String index, String type, User user){
		SearchResponse response = client.prepareSearch(index)
                .setTypes(type)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery("username",user.getUsername()))
                .get();
		return response;
	}
	/**
	* 
    * @param index type User
	* @return  user_id
	*/
	@Override
	public String getId(String index, String type, User user){
		String id = "";
		try {
			id = getSearchResponse(index,type,user).getHits().getHits()[0].getId();
		}catch(ArrayIndexOutOfBoundsException e){
			e.printStackTrace();
		}
		return(id);
	}
	/**
	* look at the existence of a user
    * @param index type User
	* @return  boolean
	*/
	@Override
	public boolean exist(String index, String type, User user){
		
		return ( 0 < getSearchResponse(index,type,user).getHits().getHits().length);
	}
	
	
	/**
	* connection method
    * @param index type userName password
	* @return  user
	*/
	public User connect(String index, String type,String userName, String password){
		User user=null;
		String query = userName+" "+password;
		
		SearchResponse response = client.prepareSearch(index)
                                  .setTypes(type)
                                  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                                  .setQuery(QueryBuilders.multiMatchQuery(query,"username","password")
                                		                 .type(Type.CROSS_FIELDS)
                                		                 .operator(Operator.AND)).get();
		client.admin().indices().prepareRefresh(index).get();
		
		SearchHit[] res = response.getHits().getHits();
		if(res.length == 1)
			user = (User)super.getObj(res[0],User.class);
		return (user);
	}
	
	/**
	* delete method
    * @param index user
	* @return  status
	*/
	@Override
	public int delete(String index,User user){
		BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
			                            .filter(QueryBuilders.matchQuery("username",user.getUsername())) 
			                            .source(index)                                  
			                            .get();                                             
		client.admin().indices().prepareRefresh(index).get();
		
		return((int)response.getDeleted());
	}
}
