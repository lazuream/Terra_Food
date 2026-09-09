package com.dayan.food.service.impl;
import com.dayan.food.entity.dto.FoodTagCreateDTO;
import com.dayan.food.entity.vo.FoodTagVO;
import com.dayan.food.entity.vo.FoodTagPageVO;
import com.dayan.food.entity.dto.FoodTagAdminUpdateDTO;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.FoodTagMapper;
import com.dayan.food.service.FoodTagService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.text.Normalizer;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service public class FoodTagServiceImpl implements FoodTagService {
 private final FoodTagMapper mapper; private final AppUserMapper users;
 public FoodTagServiceImpl(FoodTagMapper mapper, AppUserMapper users){this.mapper=mapper;this.users=users;}
 public List<FoodTagVO> list(String type,String keyword){return mapper.findApproved(type==null?null:type.toUpperCase(),keyword==null?null:keyword.trim());}
 @Transactional public FoodTagVO create(FoodTagCreateDTO request,String username){
  String type=type(request.type()); var user=requireUser(username); String name=name(request.name()); String normalized=normalize(name);
  if(mapper.countCreatedToday(user.getId())>=20) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"今天创建的待审核标签已达上限");
  try{mapper.insert(type,name,normalized,user.getId());}catch(DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"标签已存在");}
  FoodTagVO created=mapper.findByName(type,normalized); mapper.insertAudit(created.id(),user.getId(),"CREATE","用户创建待审核标签"); return created;
 }
 public List<FoodTagVO> forFood(Long foodId,String username){Long userId=username==null?null:requireUser(username).getId();return mapper.findForFood(foodId,userId);}
 @Transactional public void replaceFoodTags(Long foodId,List<Long> tagIds,String username){
  var user=requireUser(username); List<Long> ids=tagIds==null?List.of():tagIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
  if(ids.size()>30) throw new IllegalArgumentException("每道菜最多选择 30 个标签");
  if(!ids.isEmpty() && mapper.countUsable(ids,user.getId())!=ids.size()) throw new IllegalArgumentException("包含不存在、未通过或无权使用的标签");
  for(String type:List.of("TASTE","INGREDIENT","CUISINE")) if(!ids.isEmpty()&&mapper.countTypeForIds(ids,type)>10) throw new IllegalArgumentException("每类标签最多选择 10 个");
  mapper.deleteLinks(foodId); if(!ids.isEmpty()) mapper.insertLinks(foodId,ids);
 }
 public FoodTagPageVO adminList(String status,String type,String keyword,int page,int pageSize){int size=Math.min(Math.max(pageSize,1),50);int total=mapper.countAdmin(blank(status),blank(type),blank(keyword));int pages=Math.max(1,(int)Math.ceil((double)total/size));int p=Math.min(Math.max(page,1),pages);return new FoodTagPageVO(mapper.findAdmin(blank(status),blank(type),blank(keyword),(p-1)*size,size),total,p,size);}
 @Transactional public FoodTagVO adminUpdate(Long id,FoodTagAdminUpdateDTO request,String username){
  var actor=requireUser(username); var existing=requireTag(id); String newName=name(request.name()); String newType=type(request.type()); String status=status(request.status());
  if("REJECTED".equals(status) && (request.reason()==null || request.reason().isBlank())) throw new IllegalArgumentException("拒绝标签必须填写原因");
  boolean dangerous=!existing.type().equals(newType)||"DISABLED".equals(status);
  if(dangerous && actor.getRole()!=com.dayan.food.entity.enums.UserRole.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"仅主管理员可以调整分类或停用标签");
  if(mapper.updateDefinition(id,newName,normalize(newName),newType,status,actor.getId(),request.version())!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"标签已被其他管理员修改");
  if(!existing.name().equals(newName)) mapper.insertAlias(id,existing.name(),normalize(existing.name()));
  mapper.insertAudit(id,actor.getId(),"UPDATE",request.reason()); return mapper.findById(id);
 }
 @Transactional public FoodTagVO merge(Long sourceId,Long targetId,int version,String username){
  if(sourceId.equals(targetId)) throw new IllegalArgumentException("标签不能合并到自身"); var actor=requireUser(username);var source=requireTag(sourceId);var target=requireTag(targetId);
  if(!source.type().equals(target.type())||!"APPROVED".equals(target.status())) throw new IllegalArgumentException("只能合并到同类型的已通过标签");
  mapper.migrateLinks(sourceId,targetId);mapper.deleteLinksForTag(sourceId);mapper.insertAlias(targetId,source.name(),normalize(source.name()));
  if(mapper.markMerged(sourceId,targetId,actor.getId(),version)!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"标签已被其他管理员修改");
  mapper.insertAudit(sourceId,actor.getId(),"MERGE","合并到标签 "+targetId);return mapper.findById(sourceId);
 }
 private com.dayan.food.entity.po.AppUser requireUser(String username){var user=users.findByUsername(username);if(user==null||!user.isActive())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"用户不存在或已停用");return user;}
 private FoodTagVO requireTag(Long id){var tag=mapper.findById(id);if(tag==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"标签不存在");return tag;}
 private String type(String value){String type=value==null?"":value.trim().toUpperCase();if(!List.of("TASTE","INGREDIENT","CUISINE").contains(type))throw new IllegalArgumentException("标签类型不合法");return type;}
 private String status(String value){String status=value==null?"":value.trim().toUpperCase();if(!List.of("PENDING","APPROVED","REJECTED","DISABLED").contains(status))throw new IllegalArgumentException("标签状态不合法");return status;}
 private String name(String value){String name=Normalizer.normalize(value.trim(),Normalizer.Form.NFKC).replaceAll("\\s+"," ");if(name.isBlank()||name.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("标签名称不合法");return name;}
 private String normalize(String value){return name(value).toLowerCase(java.util.Locale.ROOT);}
 private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
}
