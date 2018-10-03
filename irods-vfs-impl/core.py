import os

from pg import DB

def touch(_path, _callback):
	_callback.writeLine("serverLog", "touching {} ...".format(_path))
	junk = ''
	res = _callback.msiGetSystemTime(junk, '')
	timestamp = res['arguments'][0]
	db = DB(dbname='ICAT', host='localhost', port=5432, user='irods', passwd='testpassword')
	query = db.query("update r_coll_main set modify_ts = '{}' where coll_name = '{}'".format(timestamp, _path))
	_callback.writeLine("serverLog", "rows updated = {}".format(query))

def pep_api_data_obj_put_post(rule_args, callback, rei):
	callback.writeLine("serverLog", "data_obj_put")
	data_obj_input = rule_args[2]
	callback.writeLine("serverLog", "object path = {}".format(str(data_obj_input.objPath)))
	touch(os.path.dirname(str(data_obj_input.objPath)), callback)

# Is triggered when a collection is renamed too.
def pep_api_data_obj_rename_post(rule_args, callback, rei):
	callback.writeLine("serverLog", "data_obj_rename")
	data_obj_copy_input = rule_args[2]
	path = data_obj_copy_input.destDataObjInp.objPath
	callback.writeLine("serverLog", "object path = {}".format(path))
	touch(os.path.dirname(str(path)), callback)

# How do I convert the l1descInx to a path?
def pep_api_data_obj_close_post(rule_args, callback, rei):
	callback.writeLine("serverLog", "data_obj_close")
	opened_data_obj_input = rule_args[2]
	callback.writeLine("serverLog", "cond. input = {}".format(str(opened_data_obj_input.condInput)))
	if (opened_data_obj_input.bytesWritten > 0):
		#callback.writeLine("serverLog", "object path = {}".format(str(data_obj_input.objPath)))
		touch(os.path.dirname(str(data_obj_input.objPath)), callback)

def pep_api_data_obj_unlink_post(rule_args, callback, rei):
	callback.writeLine("serverLog", "data_obj_unlink")
	data_obj_input = rule_args[2]
	callback.writeLine("serverLog", "object path = {}".format(str(data_obj_input.objPath)))
	touch(os.path.dirname(str(data_obj_input.objPath)), callback)

def pep_api_coll_create_post(rule_args, callback, rei):
	callback.writeLine("serverLog", "coll_create")
	coll_input = rule_args[2]
	callback.writeLine("serverLog", "coll. name = {}".format(str(coll_input.collName)))
	touch(os.path.dirname(str(coll_input.collName)), callback)

# Is not triggered when a collection is renamed.
def pep_api_coll_rename_post(rule_args, callback, rei):
	callback.writeLine("serverLog", "coll_rename")
	for arg in rule_args:
		callback.writeLine("serverLog", "arg: {0}".format(str(arg)))

# Is not triggered when a collection is removed.
def pep_api_mod_coll_post(rule_args, callback, rei):
	callback.writeLine("serverLog", "mod_coll")
	for arg in rule_args:
		callback.writeLine("serverLog", "arg: {0}".format(str(arg)))

def pep_api_rm_coll_post(rule_args, callback, rei):
	callback.writeLine("serverLog", "rm_coll")
	coll_input = rule_args[2]
	callback.writeLine("serverLog", "coll. name = {}".format(str(coll_input.collName)))
	touch(os.path.dirname(str(coll_input.collName)), callback)
