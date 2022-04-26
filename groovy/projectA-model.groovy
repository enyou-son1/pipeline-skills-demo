 
def getUserInfo(args) {
	def name = args.name
	def age = args.age
	def phone = args.phone
	def address = args.address
	def email = args.email
	def gender = args.gender
	def is_marry = args.is_marry
	
	// she or he
	def binge = (gender == "male")? "he" : "she"
	// if is marry
	def marry = (is_marry == true)? "is marry" : "is not marry yet"
	def userInfo = """
	${name} come from ${address}, ${binge} is ${age} old. ${binge}'s phone number is
	${phone}, or you can contact ${binge} via ${email}, ${binge} ${marry}.
	"""
	println userInfo
}
 
return this;