<?xml version="1.0" encoding="UTF-8"?>

<!--
	Document   : XmlpgToXmlSchemaXsd.xslt.xsl
	Created on : August 25, 2018, 3:29 PM
	Author     : Don Brutzman
	Description: Convert legacy ad-hoc XML to XML Schema .xsd form
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
				xmlns:xs ="http://www.w3.org/2001/XMLSchema"
				xmlns:xalan="http://xml.apache.org/xslt">
	<xsl:output encoding="UTF-8" media-type="text/xml" indent="yes" omit-xml-declaration="no" method="xml" xalan:indent-amount="4"/>
	<xsl:strip-space elements="*" />

	<xsl:template match="/">
		<xsl:text>&#10;</xsl:text>	

		<xs:schema version="1.0"
				   xmlns:xs="http://www.w3.org/2001/XMLSchema"
				   elementFormDefault="qualified">

			<xsl:apply-templates select="*"/>
		</xs:schema>
		<xsl:text>&#10;</xsl:text>
	
	</xsl:template>
  
	<xsl:template match="class">
	    <xsl:variable name="className" select="@name"/>
		<xsl:variable name="baseType">
			<xsl:choose>
				<xsl:when test="(normalize-space(@inheritsFrom) = 'root')">
					<!-- no inheritance -->
				</xsl:when>
				<xsl:when test="(string-length(normalize-space(@inheritsFrom)) > 0)">
					<xsl:value-of select="normalize-space(@inheritsFrom)"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text></xsl:text><!-- SomePduType -->
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
	    <xsl:variable name="isAbstract" select="boolean(//class[@inheritsFrom = $className])"/>
	    <xsl:variable name="pduType"        select="initialValue[@name = 'pduType']/@value"/>
	    <xsl:variable name="protocolFamily" select="initialValue[@name = 'protocolFamily']/@value"/><!-- TODO -->
		
		<xsl:choose>
			<xsl:when test="preceding::class[@name = $className]">
				<xsl:message>
					<xsl:text>*** duplicate class name='</xsl:text>
					<xsl:value-of select="@name"/>
					<xsl:text>' found, ignored</xsl:text>
				</xsl:message>
			</xsl:when>
			<xsl:when test="$isAbstract">
				<xsl:message>
					<xsl:text>*** $isAbstract=true complexType name='</xsl:text>
					<xsl:value-of select="$className"/>
					<xsl:text>' found</xsl:text>
				</xsl:message>
				<xsl:element name="xs:complexType">
					<xsl:attribute name="name">
						<xsl:value-of select="$className"/>
					</xsl:attribute>
				</xsl:element>
			</xsl:when>
			<xsl:when test="(//attribute/classRef[@name = $className]) and false()"><!-- TODO -->
				<!-- something else refers to this element -->
				<xsl:message>
					<xsl:text>*** element name='</xsl:text>
					<xsl:value-of select="$className"/>
					<xsl:text>' found, TODO</xsl:text>
				</xsl:message>
				<xsl:element name="xs:element">
					<xsl:attribute name="name">
						<xsl:value-of select="$className"/>
					</xsl:attribute>
					<xsl:attribute name="abstract">
						<xsl:value-of select="$isAbstract"/>
					</xsl:attribute>
					<xsl:if test="(string-length(normalize-space(@comment)) > 0)">
						<xs:annotation>
							<xs:appinfo>
								<xsl:value-of select="normalize-space(@comment)"/>
							</xs:appinfo>
						</xs:annotation>
					</xsl:if>
					<xsl:if test="attribute">
						<xs:complexType>
							<xsl:choose>
								<xsl:when test="(string-length($baseType) > 0)">
									<xs:complexContent>
										<xs:extension base="{$baseType}">
											<xsl:if test="attribute[classRef]">
												<!-- TODO warning -->
											</xsl:if>
											<xsl:apply-templates select="attribute[not(classRef)]">
												<!-- contained attributes -->
												<xsl:sort select="@name" order="ascending" data-type="text"/>
											</xsl:apply-templates>
										</xs:extension>
									</xs:complexContent>
								</xsl:when>
								<xsl:otherwise>
									<!-- no xs:complexContent/xs:extension -->
									<xsl:if test="attribute[classRef]">
										<!-- TODO warning -->
									</xsl:if>
									<xsl:apply-templates select="attribute[not(classRef)]">
										<!-- contained attributes -->
										<xsl:sort select="@name" order="ascending" data-type="text"/>
									</xsl:apply-templates>
								</xsl:otherwise>
							</xsl:choose>
						</xs:complexType>
					</xsl:if>
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="xs:element">
					<xsl:attribute name="name">
						<xsl:value-of select="$className"/>
					</xsl:attribute>
					<xsl:attribute name="abstract">
						<xsl:value-of select="$isAbstract"/>
					</xsl:attribute>
					<xsl:if test="(string-length(normalize-space(@comment)) > 0)">
						<xs:annotation>
							<xs:appinfo>
								<xsl:value-of select="normalize-space(@comment)"/>
							</xs:appinfo>
						</xs:annotation>
					</xsl:if>
					<xsl:if test="attribute">
						<xs:complexType>
							<xsl:choose>
								<xsl:when test="(string-length($baseType) > 0)">
									<xs:complexContent>
										<xs:extension base="{$baseType}">
											<xsl:if test="attribute[classRef]">
												<xs:choice>
													<xsl:apply-templates select="attribute[classRef]">
														<!-- contained elements -->
														<xsl:sort select="@name" order="ascending" data-type="text"/>
													</xsl:apply-templates>
												</xs:choice>
											</xsl:if>
											<xsl:apply-templates select="attribute[not(classRef)]">
												<!-- contained attributes -->
												<xsl:sort select="@name" order="ascending" data-type="text"/>
											</xsl:apply-templates>
											<xsl:if test="(string-length($pduType) > 0)">
												<xsl:element name="xs:attribute">
													<xsl:attribute name="name">
														<xsl:text>pduType</xsl:text>
													</xsl:attribute>
													<xsl:attribute name="fixed">
														<xsl:value-of select="$pduType"/>
													</xsl:attribute>
													<xsl:attribute name="type">
														<xsl:text>xs:short</xsl:text>
													</xsl:attribute>
												</xsl:element>
											</xsl:if>
										</xs:extension>
									</xs:complexContent>
								</xsl:when>
								<xsl:otherwise>
									<xsl:if test="attribute[classRef]">
										<!-- TODO warning -->
									</xsl:if>
											<xsl:apply-templates select="attribute[not(classRef)]">
												<!-- contained attributes -->
												<xsl:sort select="@name" order="ascending" data-type="text"/>
											</xsl:apply-templates>
											<xsl:if test="(string-length($pduType) > 0)">
												<xsl:element name="xs:attribute">
													<xsl:attribute name="name">
														<xsl:text>pduType</xsl:text>
													</xsl:attribute>
													<xsl:attribute name="fixed">
														<xsl:value-of select="$pduType"/>
													</xsl:attribute>
													<xsl:attribute name="type">
														<xsl:text>xs:short</xsl:text>
													</xsl:attribute>
												</xsl:element>
											</xsl:if>
								</xsl:otherwise>
							</xsl:choose>
						</xs:complexType>
					</xsl:if>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="attribute[classRef]">
		<!-- this is not an attribute, rather this is a contained element -->
	    <xsl:variable name="fieldName" select="@name"/>
		<xsl:variable name="classRefName" select="classRef/@name"/>
		
		<xsl:choose>
			<xsl:when test="(count(following-sibling::*/classRef[@name = $classRefName]) > 0)">
				<xsl:comment> 
					<xsl:text> field name </xsl:text>
					<xsl:value-of select="$fieldName"/> 
					<xsl:text>: </xsl:text>
				</xsl:comment>
			</xsl:when>
			<xsl:otherwise>
				<xsl:comment>
					<xsl:text> field name </xsl:text>
					<xsl:value-of select="$fieldName"/> 
					<xsl:text>: </xsl:text>
				</xsl:comment>
				<xsl:element name="xs:element">
					<xsl:attribute name="ref">
						<xsl:value-of select="$classRefName"/>
					</xsl:attribute>
				</xsl:element>
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>

	<xsl:template match="attribute">
	    <xsl:variable name="attributeName" select="@name"/>
		<!-- debug -->
		<xsl:if test="(string-length(normalize-space(primitive/@type)) = 0)">
			<xsl:message>
				<xsl:text>*** set missing type='xs:string' for attribute name='</xsl:text>
				<xsl:value-of select="$attributeName"/>
				<xsl:text>'</xsl:text>
			</xsl:message>
		</xsl:if>
		<xsl:variable name="attributeType">
			<xsl:call-template name="convert-type">
				<xsl:with-param name="xmlpgType">
					<xsl:value-of select="primitive/@type"/>
				</xsl:with-param>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="defaultValue">
			<xsl:value-of select="normalize-space(primitive/@defaultValue)"/>
		</xsl:variable>

		<xsl:element name="xs:attribute">
			<xsl:attribute name="name">
				<xsl:value-of select="$attributeName"/>
			</xsl:attribute>
			<xsl:attribute name="type">
				<xsl:value-of select="$attributeType"/>
			</xsl:attribute>
			<xsl:if test="(string-length($defaultValue) > 0)">
				<xsl:attribute name="default">
					<xsl:value-of select="$defaultValue"/>
				</xsl:attribute>
			</xsl:if>
					<xsl:if test="(string-length(normalize-space(@comment)) > 0)">
						<xs:annotation>
							<xs:appinfo>
								<xsl:value-of select="normalize-space(@comment)"/>
							</xs:appinfo>
						</xs:annotation>
					</xsl:if>
		</xsl:element>
	</xsl:template>

	<xsl:template name="convert-type">
		<xsl:param name="xmlpgType"/>
		<!-- debug
		<xsl:message>
			<xsl:text>*** convert-type $xmlpgType='</xsl:text>
			<xsl:value-of select="$xmlpgType"/>
			<xsl:text>'</xsl:text>
		</xsl:message> -->
		
		<xsl:choose>
			<xsl:when test="($xmlpgType = 'unsigned byte')">
				<xsl:text>xs:unsignedByte</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'unsigned short')">
				<xsl:text>xs:unsignedShort</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'unsigned long')">
				<xsl:text>xs:unsignedLong</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'unsigned int')">
				<xsl:text>xs:unsignedInt</xsl:text>
			</xsl:when>
			<xsl:when test="(string-length(normalize-space($xmlpgType)) = 0)">
				<xsl:text>xs:string</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'byte')">
				<xsl:text>xs:byte</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'short')">
				<xsl:text>xs:short</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'long')">
				<xsl:text>xs:long</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'int')">
				<xsl:text>xs:int</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'float')">
				<xsl:text>xs:float</xsl:text>
			</xsl:when>
			<xsl:when test="($xmlpgType = 'double')">
				<xsl:text>xs:double</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:message>
					<xsl:text>*** found unknown xmlpg type='</xsl:text>
					<xsl:value-of select="$xmlpgType"/>
					<xsl:text>'</xsl:text>
				</xsl:message>
			</xsl:otherwise>
		</xsl:choose>

	</xsl:template>

</xsl:stylesheet>
